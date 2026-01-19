package com.example.IndiChessBackend.model;

public enum MatchStatus {

    IN_PROGRESS,
    WHITE_WON,
    BLACK_WON,
    DRAW,
    RESIGNED,
    TIMEOUT,
    ABANDONED;

    public boolean isFinished() {
        return this != IN_PROGRESS;
    }
}
