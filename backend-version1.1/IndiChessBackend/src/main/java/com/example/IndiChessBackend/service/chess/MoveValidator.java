package com.example.IndiChessBackend.service.chess;

public class MoveValidator {

    /**
     * Validates pawn forward moves (1 or 2 squares, no capture).
     */
    public static boolean isLegalPawnMove(
            String[][] board,
            int fromRow,
            int fromCol,
            int toRow,
            int toCol,
            boolean isWhite
    ) {

        // Must stay in same file (no capture here)
        if (fromCol != toCol) {
            return false;
        }

        int direction = isWhite ? -1 : 1;
        int startRow  = isWhite ? 6 : 1;

        // One-square forward
        if (toRow == fromRow + direction) {
            return isEmpty(board, toRow, toCol);
        }

        // Two-square forward from start
        if (fromRow == startRow && toRow == fromRow + (2 * direction)) {
            int intermediateRow = fromRow + direction;
            return isEmpty(board, intermediateRow, toCol)
                    && isEmpty(board, toRow, toCol);
        }

        return false;
    }

    /**
     * Validates pawn diagonal capture (NO en-passant, NO promotion).
     */
    public static boolean isLegalPawnCapture(
            String[][] board,
            int fromRow,
            int fromCol,
            int toRow,
            int toCol,
            boolean isWhite
    ) {

        int direction = isWhite ? -1 : 1;

        // Must move exactly 1 row forward
        if (toRow != fromRow + direction) {
            return false;
        }

        // Must move diagonally by 1 column
        if (Math.abs(toCol - fromCol) != 1) {
            return false;
        }

        String target = board[toRow][toCol];

        // Capture requires opponent piece
        if (target == null || target.isEmpty()) {
            return false;
        }

        boolean targetIsWhite = Character.isUpperCase(target.charAt(0));
        return targetIsWhite != isWhite;
    }

    private static boolean isEmpty(String[][] board, int row, int col) {
        return board[row][col] == null || board[row][col].isEmpty();
    }
}
