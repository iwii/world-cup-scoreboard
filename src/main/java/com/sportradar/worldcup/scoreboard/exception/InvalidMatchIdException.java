package com.sportradar.worldcup.scoreboard.exception;

public class InvalidMatchIdException extends ScoreboardException {
    public InvalidMatchIdException(String message, IllegalArgumentException e) {
        super(message);
        initCause(e);
    }
}
