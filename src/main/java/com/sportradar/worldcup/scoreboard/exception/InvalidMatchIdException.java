package com.sportradar.worldcup.scoreboard.exception;

public class InvalidMatchIdException extends ScoreboardException {
    private static final long serialVersionUID = 1L;

    public InvalidMatchIdException(String message) {
        super(message);
    }

    public InvalidMatchIdException(String message, Throwable cause) {
        super(message, cause);
    }
}