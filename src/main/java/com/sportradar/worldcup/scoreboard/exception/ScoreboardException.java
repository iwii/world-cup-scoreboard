package com.sportradar.worldcup.scoreboard.exception;

public abstract class ScoreboardException extends RuntimeException {
    protected ScoreboardException(String message) {
        super(message);
    }
}