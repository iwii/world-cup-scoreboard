package com.sportradar.worldcup.exception;

public class MatchNotFoundException extends ScoreboardException {
    public MatchNotFoundException(String message) {
        super(message);
    }
}