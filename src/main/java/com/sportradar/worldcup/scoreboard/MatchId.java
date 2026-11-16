package com.sportradar.worldcup.scoreboard;

import com.sportradar.worldcup.scoreboard.exception.InvalidMatchIdException;

import java.util.Objects;
import java.util.UUID;

public record MatchId(UUID value) {
    // Compact Constructor for validation in record
    public MatchId {
        Objects.requireNonNull(value, "MatchId value cannot be null");
    }
    /**
     * Creates a new random match identifier using UUID.randomUUID().
     */
    static MatchId random() {
        return new MatchId(UUID.randomUUID());
    }

    /**
     * Helper factory to create a MatchId from a raw String (e.g., for parsing or tests).
     */
    public static MatchId fromString(String value) {
        Objects.requireNonNull(value, "MatchId string cannot be null");
        try {
            return new MatchId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new InvalidMatchIdException("Invalid MatchId format: " + value, e);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
