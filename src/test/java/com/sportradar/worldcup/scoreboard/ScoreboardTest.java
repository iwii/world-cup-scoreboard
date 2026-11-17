package com.sportradar.worldcup.scoreboard;

import com.sportradar.worldcup.scoreboard.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ScoreboardTest {

    private Scoreboard scoreboard;

    @BeforeEach
    void setUp() {
        scoreboard = new Scoreboard();
    }

    @Test
    @DisplayName("Should successfully start a match with 0-0 score and return unique MatchId")
    void shouldStartMatchSuccessfully() {
        MatchId matchId = scoreboard.startMatch("Poland", "Argentina").id();

        assertThat(matchId).isNotNull();
        assertThat(matchId.value()).isNotNull();

        List<MatchSummary> summary = scoreboard.getSummary();
        assertThat(summary).hasSize(1);
        assertThat(summary.get(0))
                .satisfies(m -> {
                    assertThat(m.id()).isEqualTo(matchId);
                    assertThat(m.team1()).isEqualTo("Poland");
                    assertThat(m.team2()).isEqualTo("Argentina");
                    assertThat(m.team1Score()).isEqualTo(0);
                    assertThat(m.team2Score()).isEqualTo(0);
                });
    }

    @Test
    @DisplayName("startMatch should return a different id for each started match")
    void startMatchShouldReturnUniqueIds() {
        MatchId first = scoreboard.startMatch("Mexico", "Canada").id();
        MatchId second = scoreboard.startMatch("Spain", "Brazil").id();

        assertThat(first).isNotEqualTo(second);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should throw InvalidTeamException when team name is null, empty or blank")
    void shouldRejectInvalidTeamNames(String invalidTeam) {
        assertThatThrownBy(() -> scoreboard.startMatch(invalidTeam, "Brazil"))
                .isInstanceOf(InvalidTeamException.class);

        assertThatThrownBy(() -> scoreboard.startMatch("Brazil", invalidTeam))
                .isInstanceOf(InvalidTeamException.class);
    }

    @Test
    @DisplayName("Should throw InvalidTeamException when a team plays against itself (case-insensitive)")
    void shouldRejectSameTeamPlayingAgainstItself() {
        assertThatThrownBy(() -> scoreboard.startMatch("Germany", "GERMANY"))
                .isInstanceOf(InvalidTeamException.class);

        assertThatThrownBy(() -> scoreboard.startMatch("  France ", "france"))
                .isInstanceOf(InvalidTeamException.class);
    }

    @Test
    @DisplayName("Should successfully update score with absolute values, including downward corrections")
    void shouldUpdateScoreSuccessfully() {
        MatchId matchId = scoreboard.startMatch("Spain", "Italy").id();

        // First update
        scoreboard.updateScore(matchId, 2, 1);

        List<MatchSummary> summary = scoreboard.getSummary();
        assertThat(summary.get(0).team1Score()).isEqualTo(2);
        assertThat(summary.get(0).team2Score()).isEqualTo(1);

        // VAR correction (downward update)
        scoreboard.updateScore(matchId, 1, 1);

        summary = scoreboard.getSummary();
        assertThat(summary.get(0).team1Score()).isEqualTo(1);
        assertThat(summary.get(0).team2Score()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should throw InvalidScoreException when negative scores are provided")
    void shouldRejectNegativeScores() {
        MatchId matchId = scoreboard.startMatch("Brazil", "Croatia").id();

        assertThatThrownBy(() -> scoreboard.updateScore(matchId, -1, 0))
                .isInstanceOf(InvalidScoreException.class);

        assertThatThrownBy(() -> scoreboard.updateScore(matchId, 0, -2))
                .isInstanceOf(InvalidScoreException.class);
    }

    @Test
    @DisplayName("Should successfully finish a match and remove it from the active summary")
    void shouldFinishMatchSuccessfully() {
        MatchId matchId1 = scoreboard.startMatch("Team A", "Team B").id();
        MatchId matchId2 = scoreboard.startMatch("Team C", "Team D").id();

        scoreboard.finishMatch(matchId1);

        List<MatchSummary> summary = scoreboard.getSummary();
        assertThat(summary).hasSize(1);
        assertThat(summary.get(0).id()).isEqualTo(matchId2);
    }

    @Test
    @DisplayName("Should throw MatchNotFoundException when updating or finishing a non-existent or finished match")
    void shouldThrowExceptionForUnknownOrFinishedMatch() {
        MatchId unknownId = MatchId.random();
        MatchId matchId = scoreboard.startMatch("Japan", "Senegal").id();

        // Unknown match operations
        assertThatThrownBy(() -> scoreboard.updateScore(unknownId, 1, 0))
                .isInstanceOf(MatchNotFoundException.class);

        assertThatThrownBy(() -> scoreboard.finishMatch(unknownId))
                .isInstanceOf(MatchNotFoundException.class);

        // Finish the match once
        scoreboard.finishMatch(matchId);

        // Operations after match is finished
        assertThatThrownBy(() -> scoreboard.updateScore(matchId, 1, 0))
                .isInstanceOf(MatchNotFoundException.class);

        assertThatThrownBy(() -> scoreboard.finishMatch(matchId))
                .isInstanceOf(MatchNotFoundException.class);
    }

    @Test
    @DisplayName("Whitespace trimming and case-insensitivity on team names")
    void shouldTrimWhitespaceAndHandleCaseInsensitivity() {
        // Given teams name with spaces and mixed casing
        MatchId matchId = scoreboard.startMatch("  Brazil  ", "ARGENTINA ").id();

        List<MatchSummary> summary = scoreboard.getSummary();
        assertThat(summary).hasSize(1);
        assertThat(summary.get(0).team1()).isEqualTo("Brazil");
        assertThat(summary.get(0).team2()).isEqualTo("ARGENTINA");

        // Verify that self-play check ignores case and whitespace
        assertThatThrownBy(() -> scoreboard.startMatch("  brazil ", "BRAZIL"))
                .isInstanceOf(InvalidTeamException.class);
    }

    @Test
    @DisplayName("Should order summary by total score descending, and then by most recently started match")
    void shouldOrderSummaryCorrectly() {
        MatchId match1 = scoreboard.startMatch("Mexico", "Canada").id(); // 0:0 (started first)
        MatchId match2 = scoreboard.startMatch("Spain", "Brazil").id();   // 0:0
        MatchId match3 = scoreboard.startMatch("Germany", "France").id(); // 0:0 (started last)

        // When: Scores are updated
        scoreboard.updateScore(match1, 0, 2); // Total: 2
        scoreboard.updateScore(match2, 10, 2); // Total: 12 (Highest)
        scoreboard.updateScore(match3, 1, 1); // Total: 2 (Same as match1, but newer)

        // Then: Summary should be ordered by total score desc, then newest first
        List<MatchSummary> summary = scoreboard.getSummary();

        assertThat(summary).hasSize(3);

        // 1st: Spain vs Brazil (Total 12)
        assertThat(summary.get(0).id()).isEqualTo(match2);

        // 2nd: Germany vs France (Total 2, started *later* than Mexico-Canada)
        assertThat(summary.get(1).id()).isEqualTo(match3);

        // 3rd: Mexico vs Canada (Total 2, started *earlier*)
        assertThat(summary.get(2).id()).isEqualTo(match1);
    }

    @Test
    @DisplayName("updateScore on a finished match throws MatchNotFoundException")
    void shouldRejectUpdateOnFinishedMatch() {
        MatchId matchId = scoreboard.startMatch("Japan", "Senegal").id();
        scoreboard.finishMatch(matchId);

        assertThatThrownBy(() -> scoreboard.updateScore(matchId, 1, 0))
                .isInstanceOf(MatchNotFoundException.class)
                .hasMessageContaining(matchId.toString());
    }

    @Test
    @DisplayName("finishMatch on an already finished match throws MatchNotFoundException")
    void shouldRejectSecondFinish() {
        MatchId matchId = scoreboard.startMatch("Japan", "Senegal").id();
        scoreboard.finishMatch(matchId);

        assertThatThrownBy(() -> scoreboard.finishMatch(matchId))
                .isInstanceOf(MatchNotFoundException.class)
                .hasMessageContaining(matchId.toString());
    }

    @Test
    @DisplayName("updateScore throws NullPointerException when matchId is null")
    void updateScoreShouldRejectNullMatchId() {
        assertThatThrownBy(() -> scoreboard.updateScore(null, 1, 0))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("finishMatch throws NullPointerException when matchId is null")
    void finishMatchShouldRejectNullMatchId() {
        assertThatThrownBy(() -> scoreboard.finishMatch(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("getMatchesByTeam returns only matches involving the given team")
    void getMatchesByTeamShouldFilterByTeam() {
        MatchSummary mexicoCanada = scoreboard.startMatch("Mexico", "Canada");
        scoreboard.startMatch("Spain", "Brazil");
        scoreboard.updateScore(mexicoCanada.id(), 0, 5);

        assertThat(scoreboard.getMatchesByTeam("Canada"))
                .extracting(MatchSummary::id)
                .containsExactly(mexicoCanada.id());

        assertThat(scoreboard.getMatchesByTeam("Brazil"))
                .extracting(MatchSummary::team1, MatchSummary::team2)
                .containsExactly(tuple("Spain", "Brazil"));
    }

    @Test
    @DisplayName("getMatchesByTeam is case-insensitive and trims whitespace")
    void getMatchesByTeamShouldNormalizeInput() {
        scoreboard.startMatch("Poland", "Argentina");

        assertThat(scoreboard.getMatchesByTeam("  poland  ")).hasSize(1);
        assertThat(scoreboard.getMatchesByTeam("POLAND")).hasSize(1);
        assertThat(scoreboard.getMatchesByTeam("argentina")).hasSize(1);
    }

    @Test
    @DisplayName("getMatchesByTeam returns an empty list when the team has no ongoing matches")
    void getMatchesByTeamShouldReturnEmptyForUnknownTeam() {
        scoreboard.startMatch("Poland", "Argentina");

        assertThat(scoreboard.getMatchesByTeam("Germany")).isEmpty();
    }

    @Test
    @DisplayName("getMatchesByTeam rejects null, empty or blank team names")
    void getMatchesByTeamShouldRejectInvalidInput() {
        assertThatThrownBy(() -> scoreboard.getMatchesByTeam(null))
                .isInstanceOf(InvalidTeamException.class);
        assertThatThrownBy(() -> scoreboard.getMatchesByTeam(""))
                .isInstanceOf(InvalidTeamException.class);
        assertThatThrownBy(() -> scoreboard.getMatchesByTeam("   "))
                .isInstanceOf(InvalidTeamException.class);
    }

    @Test
    @DisplayName("getMatchesByTeam returns an immutable list")
    void getMatchesByTeamShouldReturnImmutableList() {
        scoreboard.startMatch("Poland", "Argentina");
        List<MatchSummary> result = scoreboard.getMatchesByTeam("Poland");

        assertThatThrownBy(() -> result.add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("getMatchesByTeam orders like getSummary: total descending, then most recent")
    void getMatchesByTeamShouldOrderLikeSummary() {
        MatchSummary first = scoreboard.startMatch("Poland", "Germany");
        MatchSummary second = scoreboard.startMatch("Poland", "France");
        MatchSummary third = scoreboard.startMatch("Poland", "Spain");

        scoreboard.updateScore(first.id(), 1, 1);   // total 2, started first
        scoreboard.updateScore(second.id(), 2, 2);  // total 4, started second
        scoreboard.updateScore(third.id(), 3, 0);   // total 3, started last

        assertThat(scoreboard.getMatchesByTeam("Poland"))
                .extracting(MatchSummary::id)
                .containsExactly(second.id(), third.id(), first.id());
    }

    @Test
    @DisplayName("getMatchesByTeam excludes finished matches")
    void getMatchesByTeamShouldExcludeFinishedMatches() {
        MatchSummary match = scoreboard.startMatch("Poland", "Argentina");
        scoreboard.finishMatch(match.id());

        assertThat(scoreboard.getMatchesByTeam("Poland")).isEmpty();
    }

    @Test
    @DisplayName("Should return the example scenario from the assignment in the expected order")
    void shouldReturnExampleScenarioInExpectedOrder() {
        MatchId mexicoCanada = scoreboard.startMatch("Mexico", "Canada").id();
        MatchId spainBrazil = scoreboard.startMatch("Spain", "Brazil").id();
        MatchId germanyFrance = scoreboard.startMatch("Germany", "France").id();
        MatchId uruguayItaly = scoreboard.startMatch("Uruguay", "Italy").id();
        MatchId argentinaAustralia = scoreboard.startMatch("Argentina", "Australia").id();

        scoreboard.updateScore(mexicoCanada, 0, 5);
        scoreboard.updateScore(spainBrazil, 10, 2);
        scoreboard.updateScore(germanyFrance, 2, 2);
        scoreboard.updateScore(uruguayItaly, 6, 6);
        scoreboard.updateScore(argentinaAustralia, 3, 1);

        assertThat(scoreboard.getSummary())
                .extracting(MatchSummary::team1, MatchSummary::team2,
                        MatchSummary::team1Score, MatchSummary::team2Score)
                .containsExactly(
                        tuple("Uruguay", "Italy", 6, 6),
                        tuple("Spain", "Brazil", 10, 2),
                        tuple("Mexico", "Canada", 0, 5),
                        tuple("Argentina", "Australia", 3, 1),
                        tuple("Germany", "France", 2, 2));
    }

    @Test
    @DisplayName("getSummary returns an empty list when no matches are in progress")
    void getSummaryShouldReturnEmptyListForEmptyScoreboard() {
        assertThat(scoreboard.getSummary()).isEmpty();
    }

    @Test
    @DisplayName("getSummary returns an immutable list")
    void getSummaryShouldReturnImmutableList() {
        scoreboard.startMatch("Poland", "Argentina");
        List<MatchSummary> result = scoreboard.getSummary();

        assertThatThrownBy(() -> result.add(null))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(result::clear)
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("getSummary returns a snapshot, not a live view")
    void getSummaryShouldReturnSnapshotNotLiveView() {
        MatchId matchId = scoreboard.startMatch("Poland", "Argentina").id();
        List<MatchSummary> before = scoreboard.getSummary();

        scoreboard.updateScore(matchId, 2, 1);

        assertThat(before.get(0).team1Score()).isZero();
        assertThat(scoreboard.getSummary().get(0).team1Score()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should order by total score without int overflow for very large scores")
    void shouldOrderByTotalScoreWithoutIntOverflow() {
        MatchId huge = scoreboard.startMatch("Mexico", "Canada").id();
        MatchId small = scoreboard.startMatch("Spain", "Brazil").id();

        scoreboard.updateScore(huge, Integer.MAX_VALUE, Integer.MAX_VALUE);
        scoreboard.updateScore(small, 1, 0);

        assertThat(scoreboard.getSummary())
                .extracting(MatchSummary::id)
                .containsExactly(huge, small);
    }

    @Test
    @DisplayName("updateScore returns the updated summary")
    void updateScoreShouldReturnUpdatedSummary() {
        MatchId matchId = scoreboard.startMatch("Spain", "Italy").id();

        MatchSummary updated = scoreboard.updateScore(matchId, 2, 1);

        assertThat(updated).isEqualTo(new MatchSummary(matchId, "Spain", "Italy", 2, 1));
    }

    @Test
    @DisplayName("finishMatch returns the final summary with the last accepted score")
    void finishMatchShouldReturnFinalSummary() {
        MatchId matchId = scoreboard.startMatch("Japan", "Senegal").id();
        scoreboard.updateScore(matchId, 3, 2);

        MatchSummary finalResult = scoreboard.finishMatch(matchId);

        assertThat(finalResult).isEqualTo(new MatchSummary(matchId, "Japan", "Senegal", 3, 2));
    }

    @Test
    @DisplayName("The same teams may play several ongoing matches, each with its own id")
    void shouldAllowSameTeamsInSeveralOngoingMatches() {
        MatchId first = scoreboard.startMatch("Mexico", "Canada").id();
        MatchId second = scoreboard.startMatch("Mexico", "Canada").id();

        assertThat(first).isNotEqualTo(second);
        assertThat(scoreboard.getSummary())
                .extracting(MatchSummary::id)
                .containsExactly(second, first);
    }
}