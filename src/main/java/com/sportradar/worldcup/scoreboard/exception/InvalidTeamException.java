package com.sportradar.worldcup.scoreboard.exception;

public class InvalidTeamException extends ScoreboardException {
    private static final long serialVersionUID = 1L;

    public InvalidTeamException(String message) {
        super(message);
    }

}
