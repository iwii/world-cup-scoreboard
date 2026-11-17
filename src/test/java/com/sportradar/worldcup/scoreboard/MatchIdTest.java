package com.sportradar.worldcup.scoreboard;

import com.sportradar.worldcup.scoreboard.exception.InvalidMatchIdException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchIdTest {

    @Test
    @DisplayName("fromString should parse a valid UUID string")
    void shouldParseValidUuid() {
        UUID uuid = UUID.randomUUID();

        MatchId matchId = MatchId.fromString(uuid.toString());

        assertThat(matchId.value()).isEqualTo(uuid);
    }

    @Test
    @DisplayName("fromString should reject null with NullPointerException")
    void shouldRejectNull() {
        assertThatThrownBy(() -> MatchId.fromString(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("fromString should wrap malformed UUID in InvalidMatchIdException")
    void shouldWrapMalformedUuid() {
        assertThatThrownBy(() -> MatchId.fromString("not-a-uuid"))
                .isInstanceOf(InvalidMatchIdException.class)
                .hasMessageContaining("not-a-uuid")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("fromString should reject empty string with InvalidMatchIdException")
    void shouldRejectEmptyString() {
        assertThatThrownBy(() -> MatchId.fromString(""))
                .isInstanceOf(InvalidMatchIdException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("random should return a non-null id wrapping a non-null UUID")
    void randomShouldReturnNonNullId() {
        assertThat(MatchId.random()).isNotNull();
        assertThat(MatchId.random().value()).isNotNull();
    }

    @Test
    @DisplayName("random should return a different id on each call")
    void randomShouldReturnDifferentIdsOnEachCall() {
        assertThat(MatchId.random()).isNotEqualTo(MatchId.random());
    }

    @Test
    @DisplayName("random id should survive a round trip through toString and fromString")
    void randomIdShouldSurviveRoundTripThroughString() {
        MatchId id = MatchId.random();

        assertThat(MatchId.fromString(id.toString())).isEqualTo(id);
    }

    @Test
    @DisplayName("toString should return the canonical UUID string")
    void toStringShouldReturnUuidString() {
        UUID uuid = UUID.randomUUID();

        assertThat(new MatchId(uuid)).hasToString(uuid.toString());
    }

}