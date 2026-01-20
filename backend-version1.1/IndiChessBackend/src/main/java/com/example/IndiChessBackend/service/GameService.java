package com.example.IndiChessBackend.service;

import com.example.IndiChessBackend.model.DTO.*;
import com.example.IndiChessBackend.model.Match;
import com.example.IndiChessBackend.model.Move;
import com.example.IndiChessBackend.model.PieceColor;
import com.example.IndiChessBackend.repo.MatchRepo;
import jakarta.transaction.Transactional;
import lombok.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class GameService {

    private final MatchRepo matchRepo;
    private final SimpMessagingTemplate messagingTemplate;

    /* =========================
       IN-MEMORY GAME STATE
       ========================= */

    private final Map<Long, GameState> activeGames = new ConcurrentHashMap<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class GameState {
        private String[][] board;
        private boolean whiteTurn;
        private String status;
        private String whitePlayer;
        private String blackPlayer;
        private LocalDateTime lastMoveTime;
    }

    /* =========================
       GAME SNAPSHOT (REST)
       ========================= */

    public GameDTO getGameDetails(Long matchId, HttpServletRequest request) {
        Match match = matchRepo.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        GameState state = activeGames.computeIfAbsent(
                matchId,
                id -> initializeGameFromDb(match)
        );

        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            throw new RuntimeException("Unauthenticated");
        }

        String username = principal.getName();
        boolean isWhite = username.equals(state.whitePlayer);

        GameDTO dto = new GameDTO();
        dto.setId(matchId);
        dto.setPlayer1(match.getPlayer1());
        dto.setPlayer2(match.getPlayer2());
        dto.setStatus(state.status);
        dto.setBoard(state.board);
        dto.setFen(convertBoardToFEN(state.board, state.whiteTurn));
        dto.setPlayerColor(isWhite ? "white" : "black");
        dto.setMyTurn(state.whiteTurn == isWhite);
        dto.setCreatedAt(match.getCreatedAt());

        return dto;
    }

    /* =========================
       MOVE PROCESSING (WS)
       ========================= */

    @Transactional
    public MoveDTO processMove(Long matchId, MoveRequest move, Principal principal) {

        GameState state = activeGames.computeIfAbsent(matchId, id -> {
            Match match = matchRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Match not found"));
            return initializeGameFromDb(match);
        });

        Match match = matchRepo.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        String username = principal.getName();
        boolean isWhite = username.equals(state.getWhitePlayer());

        if (state.isWhiteTurn() != isWhite) {
            throw new RuntimeException("Not your turn");
        }

        if (move.getFromRow() == null || move.getFromCol() == null ||
                move.getToRow() == null || move.getToCol() == null) {
            throw new RuntimeException("Invalid move coordinates");
        }

        String piece = state.getBoard()[move.getFromRow()][move.getFromCol()];
        if (piece == null || piece.isEmpty()) {
            throw new RuntimeException("No piece at source square");
        }

        boolean pieceIsWhite = Character.isUpperCase(piece.charAt(0));
        if (pieceIsWhite != isWhite) {
            throw new RuntimeException("You cannot move opponent pieces");
        }

        if (Character.toLowerCase(piece.charAt(0)) == 'p') {
            boolean legal = com.example.IndiChessBackend.service.chess.MoveValidator
                    .isLegalPawnMove(
                            state.getBoard(),
                            move.getFromRow(),
                            move.getFromCol(),
                            move.getToRow(),
                            move.getToCol(),
                            isWhite
                    );

            if (!legal) {
                throw new RuntimeException("Illegal pawn move");
            }
        } else {
            throw new RuntimeException("Only pawn moves supported right now");
        }

        String fenBefore = convertBoardToFEN(state.getBoard(), state.isWhiteTurn());

        state.getBoard()[move.getFromRow()][move.getFromCol()] = "";
        state.getBoard()[move.getToRow()][move.getToCol()] = piece;

        state.setWhiteTurn(!state.isWhiteTurn());
        state.setLastMoveTime(LocalDateTime.now());

        String fenAfter = convertBoardToFEN(state.getBoard(), state.isWhiteTurn());

        int nextPly = (match.getCurrentPly() == null ? 0 : match.getCurrentPly()) + 1;

        Move dbMove = new Move();
        dbMove.setMatch(match);
        dbMove.setPly(nextPly);
        dbMove.setMoveNumber((nextPly + 1) / 2);
        dbMove.setColor(PieceColor.fromPly(nextPly));
        dbMove.setFenBefore(fenBefore);
        dbMove.setFenAfter(fenAfter);
        dbMove.setUci(
                "" + (char)('a' + move.getFromCol()) + (8 - move.getFromRow()) +
                        (char)('a' + move.getToCol()) + (8 - move.getToRow())
        );

        match.getMoves().add(dbMove);
        match.setCurrentPly(nextPly);
        match.setFenCurrent(fenAfter);
        match.setLastMoveUci(dbMove.getUci());

        matchRepo.save(match);

        MoveDTO dto = new MoveDTO();
        dto.setMatchId(matchId);
        dto.setFromRow(move.getFromRow());
        dto.setFromCol(move.getFromCol());
        dto.setToRow(move.getToRow());
        dto.setToCol(move.getToCol());
        dto.setPiece(piece);
        dto.setBoard(state.getBoard());
        dto.setFenBefore(fenBefore);
        dto.setFenAfter(fenAfter);
        dto.setIsWhiteTurn(state.isWhiteTurn());
        dto.setPlayerUsername(username);
        dto.setTimestamp(LocalDateTime.now());

        messagingTemplate.convertAndSend("/topic/moves/" + matchId, dto);
        return dto;
    }

    /* =========================
       JOIN / RESIGN
       ========================= */

    public GameStatusDTO handlePlayerJoin(Long matchId, Principal principal) {
        GameState state = activeGames.get(matchId);
        if (state == null) {
            throw new RuntimeException("Game not active");
        }

        GameStatusDTO dto = new GameStatusDTO();
        dto.setMatchId(matchId);
        dto.setStatus(state.status);
        dto.setBoard(state.board);
        dto.setFen(convertBoardToFEN(state.board, state.whiteTurn));
        return dto;
    }

    public void handleResignation(Long matchId, String username) {
        GameState state = activeGames.get(matchId);
        if (state == null) return;

        state.status = "RESIGNED";

        GameStatusDTO dto = new GameStatusDTO();
        dto.setMatchId(matchId);
        dto.setStatus("RESIGNED");

        messagingTemplate.convertAndSend("/topic/game/" + matchId, dto);
    }

    /* =========================
       INITIALIZATION
       ========================= */

    private GameState initializeGameFromDb(Match match) {
        String[][] board = initialBoard();
        boolean whiteTurn = true;

        if (match.getMoves() != null && !match.getMoves().isEmpty()) {
            match.getMoves().stream()
                    .sorted((a, b) -> Integer.compare(a.getPly(), b.getPly()))
                    .forEach(move -> {
                        int fromCol = move.getUci().charAt(0) - 'a';
                        int fromRow = 8 - Character.getNumericValue(move.getUci().charAt(1));
                        int toCol = move.getUci().charAt(2) - 'a';
                        int toRow = 8 - Character.getNumericValue(move.getUci().charAt(3));

                        String piece = board[fromRow][fromCol];
                        board[fromRow][fromCol] = "";
                        board[toRow][toCol] = piece;
                    });

            whiteTurn = match.getCurrentPly() % 2 == 0;
        }

        return new GameState(
                board,
                whiteTurn,
                match.getStatus().name(),
                match.getPlayer1().getUsername(),
                match.getPlayer2().getUsername(),
                LocalDateTime.now()
        );
    }

    private String[][] initialBoard() {
        return new String[][]{
                {"r","n","b","q","k","b","n","r"},
                {"p","p","p","p","p","p","p","p"},
                {"","","","","","","",""},
                {"","","","","","","",""},
                {"","","","","","","",""},
                {"","","","","","","",""},
                {"P","P","P","P","P","P","P","P"},
                {"R","N","B","Q","K","B","N","R"}
        };
    }

    /* =========================
       FEN
       ========================= */

    private String convertBoardToFEN(String[][] board, boolean whiteTurn) {
        StringBuilder fen = new StringBuilder();

        for (int r = 0; r < 8; r++) {
            int empty = 0;
            for (int c = 0; c < 8; c++) {
                String p = board[r][c];
                if (p == null || p.isEmpty()) empty++;
                else {
                    if (empty > 0) fen.append(empty);
                    empty = 0;
                    fen.append(p);
                }
            }
            if (empty > 0) fen.append(empty);
            if (r < 7) fen.append("/");
        }

        fen.append(" ").append(whiteTurn ? "w" : "b");
        fen.append(" KQkq - 0 1");
        return fen.toString();
    }
}
