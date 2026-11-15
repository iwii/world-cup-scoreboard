package com.sportradar.worldcup.scoreboard;

import com.sportradar.worldcup.exception.InvalidTeamException;

import java.util.Objects;

record Team(String name) {

    Team {
        Objects.requireNonNull(name, "name");

        String normalizedName = name.strip();
        if (normalizedName.isEmpty()) {
            throw new InvalidTeamException("Team name must not be blank");
        }
        name = normalizedName;
    }

    boolean sameNameAs(Team other) {
        return name.equalsIgnoreCase(other.name);
    }
}