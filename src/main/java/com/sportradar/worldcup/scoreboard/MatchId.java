package com.sportradar.worldcup.scoreboard;

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
    public static MatchId random() {
        return new MatchId(UUID.randomUUID());
    }

    /**
     * Helper factory to create a MatchId from a raw String (e.g., for parsing or tests).
     */
    public static MatchId fromString(String value) {
        Objects.requireNonNull(value, "MatchId string cannot be null");
        return new MatchId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
