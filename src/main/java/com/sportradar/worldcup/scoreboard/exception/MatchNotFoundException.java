package com.sportradar.worldcup.scoreboard.exception;

public class MatchNotFoundException extends ScoreboardException {
    private static final long serialVersionUID = 1L;

    public MatchNotFoundException(String message) {
        super(message);
    }

}