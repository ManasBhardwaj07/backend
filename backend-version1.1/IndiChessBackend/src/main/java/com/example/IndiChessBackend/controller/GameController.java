package com.example.IndiChessBackend.controller;

import com.example.IndiChessBackend.model.DTO.GameDTO;
import com.example.IndiChessBackend.model.DTO.GameStatusDTO;
import com.example.IndiChessBackend.model.DTO.MoveDTO;
import com.example.IndiChessBackend.model.DTO.MoveRequest;
import com.example.IndiChessBackend.service.GameService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class GameController {

    private final GameService gameService;

    /* =========================
       REST — GAME SNAPSHOT
       ========================= */

    @GetMapping("/{matchId}")
    public ResponseEntity<GameDTO> getGame(
            @PathVariable Long matchId,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
                gameService.getGameDetails(matchId, request)
        );
    }

    /* =========================
       WEBSOCKET — MOVE
       ========================= */

    @MessageMapping("/game/{matchId}/move")
    @SendTo("/topic/moves/{matchId}")
    public MoveDTO handleMove(
            @DestinationVariable Long matchId,
            @Payload MoveRequest moveRequest,
            Principal principal
    ) {
        System.out.println("WS handleMove called, principal=" + principal);
        return gameService.processMove(matchId, moveRequest, principal);
    }


    /* =========================
       WEBSOCKET — JOIN
       ========================= */

    @MessageMapping("/game/{matchId}/join")
    @SendTo("/topic/game/{matchId}")
    public GameStatusDTO handlePlayerJoin(
            @DestinationVariable Long matchId,
            Principal principal
    ) {
        return gameService.handlePlayerJoin(matchId, principal);
    }

    /* =========================
       WEBSOCKET — RESIGN
       ========================= */

    @MessageMapping("/game/{matchId}/resign")
    public void handleResign(
            @DestinationVariable Long matchId,
            Principal principal
    ) {
        gameService.handleResignation(matchId, principal.getName());
    }
}
