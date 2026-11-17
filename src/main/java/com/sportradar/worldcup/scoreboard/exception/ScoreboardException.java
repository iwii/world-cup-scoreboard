package com.sportradar.worldcup.scoreboard.exception;

public abstract class ScoreboardException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    protected ScoreboardException(String message) {
        super(message);
    }

    protected ScoreboardException(String message, Throwable cause) {
        super(message, cause);
    }
}