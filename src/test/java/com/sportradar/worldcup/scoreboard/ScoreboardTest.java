package com.sportradar.worldcup.scoreboard;

import com.sportradar.worldcup.exception.InvalidScoreException;
import com.sportradar.worldcup.exception.InvalidTeamException;
import com.sportradar.worldcup.exception.MatchNotFoundException;
import com.sportradar.worldcup.exception.ScoreboardException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScoreboardTest {

    private final Scoreboard scoreboard = new Scoreboard();

    @Nested
    class StartMatch {

        @Test
        void startsMatchWithZeroZeroScoreAndGeneratedId() {
            MatchSummary match = scoreboard.startMatch("Mexico", "Canada");

            assertNotNull(match.id());
            assertEquals("Mexico", match.homeTeam());
            assertEquals("Canada", match.awayTeam());
            assertEquals(0, match.homeScore());
            assertEquals(0, match.awayScore());
        }

        @Test
        void trimsTeamNamesAndPreservesSpelling() {
            MatchSummary match = scoreboard.startMatch("  mExIcO  ", " Canada ");

            assertEquals("mExIcO", match.homeTeam());
            assertEquals("Canada", match.awayTeam());
        }

        @Test
        void addsStartedMatchToSummary() {
            MatchSummary match = scoreboard.startMatch("Mexico", "Canada");

            assertEquals(List.of(match), scoreboard.getSummary());
        }

        @Test
        void allowsStartingSameTeamsMoreThanOnce() {
            MatchSummary first = scoreboard.startMatch("Mexico", "Canada");
            MatchSummary second = scoreboard.startMatch("Mexico", "Canada");

            assertNotEquals(first.id(), second.id());
            assertEquals(2, scoreboard.getSummary().size());
        }

        @Test
        void allowsSameTeamToParticipateInMultipleMatches() {
            scoreboard.startMatch("Mexico", "Canada");
            scoreboard.startMatch("Mexico", "Brazil");

            assertEquals(2, scoreboard.getSummary().size());
        }

        @Test
        void generatesUniqueIds() {
            List<MatchId> ids = new ArrayList<>();

            for (int i = 0; i < 1_000; i++) {
                ids.add(scoreboard.startMatch("Home " + i, "Away " + i).id());
            }

            assertEquals(ids.size(), ids.stream().distinct().count());
        }

        @Test
        void rejectsNullHomeTeam() {
            assertThrows(
                    NullPointerException.class,
                    () -> scoreboard.startMatch(null, "Canada")
            );
        }

        @Test
        void rejectsNullAwayTeam() {
            assertThrows(
                    NullPointerException.class,
                    () -> scoreboard.startMatch("Mexico", null)
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "   ", "\t", "\n", "\u2003"})
        void rejectsBlankHomeTeamName(String teamName) {
            assertThrows(
                    InvalidTeamException.class,
                    () -> scoreboard.startMatch(teamName, "Canada")
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "   ", "\t", "\n", "\u2003"})
        void rejectsBlankAwayTeamName(String teamName) {
            assertThrows(
                    InvalidTeamException.class,
                    () -> scoreboard.startMatch("Mexico", teamName)
            );
        }

        @Test
        void rejectsTeamsWithSameNameIgnoringCaseAndWhitespace() {
            assertThrows(
                    InvalidTeamException.class,
                    () -> scoreboard.startMatch(" Mexico ", "mExIcO")
            );
        }

        @Test
        void rejectedStartDoesNotChangeScoreboard() {
            scoreboard.startMatch("Mexico", "Canada");
            List<MatchSummary> before = scoreboard.getSummary();

            assertThrows(
                    InvalidTeamException.class,
                    () -> scoreboard.startMatch("Brazil", " brazil ")
            );

            assertEquals(before, scoreboard.getSummary());
        }
    }

    @Nested
    class UpdateScore {

        private final MatchSummary startedMatch =
                scoreboard.startMatch("Mexico", "Canada");

        @Test
        void returnsUpdatedMatch() {
            MatchSummary updated = scoreboard.updateScore(
                    startedMatch.id(), 2, 1
            );

            assertEquals(2, updated.homeScore());
            assertEquals(1, updated.awayScore());
            assertEquals(startedMatch.id(), updated.id());
        }

        @Test
        void updatesScoreVisibleInSummary() {
            scoreboard.updateScore(startedMatch.id(), 2, 1);

            MatchSummary result = scoreboard.getSummary().getFirst();

            assertEquals(2, result.homeScore());
            assertEquals(1, result.awayScore());
        }

        @Test
        void treatsScoreAsAbsoluteValues() {
            scoreboard.updateScore(startedMatch.id(), 2, 1);
            MatchSummary updated = scoreboard.updateScore(
                    startedMatch.id(), 3, 2
            );

            assertEquals(3, updated.homeScore());
            assertEquals(2, updated.awayScore());
        }

        @Test
        void allowsCorrectingScoreDownwards() {
            scoreboard.updateScore(startedMatch.id(), 3, 2);

            MatchSummary updated = scoreboard.updateScore(
                    startedMatch.id(), 2, 1
            );

            assertEquals(2, updated.homeScore());
            assertEquals(1, updated.awayScore());
        }

        @Test
        void allowsUpdatingToSameScore() {
            scoreboard.updateScore(startedMatch.id(), 2, 1);

            MatchSummary updated = scoreboard.updateScore(
                    startedMatch.id(), 2, 1
            );

            assertEquals(2, updated.homeScore());
            assertEquals(1, updated.awayScore());
        }

        @Test
        void allowsUpdatingScoreToZeroZero() {
            scoreboard.updateScore(startedMatch.id(), 2, 1);

            MatchSummary updated = scoreboard.updateScore(
                    startedMatch.id(), 0, 0
            );

            assertEquals(0, updated.homeScore());
            assertEquals(0, updated.awayScore());
        }

        @Test
        void allowsMaximumIntegerScores() {
            MatchSummary updated = scoreboard.updateScore(
                    startedMatch.id(), Integer.MAX_VALUE, Integer.MAX_VALUE
            );

            assertEquals(Integer.MAX_VALUE, updated.homeScore());
            assertEquals(Integer.MAX_VALUE, updated.awayScore());
        }

        @Test
        void doesNotChangeOtherMatches() {
            MatchSummary anotherMatch =
                    scoreboard.startMatch("Brazil", "Argentina");

            scoreboard.updateScore(startedMatch.id(), 2, 1);

            MatchSummary unchanged = scoreboard.getSummary().stream()
                    .filter(match -> match.id().equals(anotherMatch.id()))
                    .findFirst()
                    .orElseThrow();

            assertEquals(0, unchanged.homeScore());
            assertEquals(0, unchanged.awayScore());
        }

        @Test
        void rejectsNegativeHomeScore() {
            assertThrows(
                    InvalidScoreException.class,
                    () -> scoreboard.updateScore(startedMatch.id(), -1, 0)
            );
        }

        @Test
        void rejectsNegativeAwayScore() {
            assertThrows(
                    InvalidScoreException.class,
                    () -> scoreboard.updateScore(startedMatch.id(), 0, -1)
            );
        }

        @Test
        void rejectsMinimumIntegerScore() {
            assertThrows(
                    InvalidScoreException.class,
                    () -> scoreboard.updateScore(
                            startedMatch.id(), Integer.MIN_VALUE, 0
                    )
            );
        }

        @Test
        void rejectedUpdateDoesNotChangeScoreboard() {
            scoreboard.updateScore(startedMatch.id(), 2, 1);
            List<MatchSummary> before = scoreboard.getSummary();

            assertThrows(
                    InvalidScoreException.class,
                    () -> scoreboard.updateScore(startedMatch.id(), -1, 0)
            );

            assertEquals(before, scoreboard.getSummary());
        }

        @Test
        void rejectsNullMatchId() {
            assertThrows(
                    NullPointerException.class,
                    () -> scoreboard.updateScore(null, 1, 0)
            );
        }

        @Test
        void rejectsUnknownMatchId() {
            MatchId unknownId = new MatchId(java.util.UUID.randomUUID());

            assertThrows(
                    MatchNotFoundException.class,
                    () -> scoreboard.updateScore(unknownId, 1, 0)
            );
        }

        @Test
        void rejectsMatchIdFromAnotherScoreboard() {
            Scoreboard anotherScoreboard = new Scoreboard();

            assertThrows(
                    MatchNotFoundException.class,
                    () -> anotherScoreboard.updateScore(
                            startedMatch.id(), 1, 0
                    )
            );
        }
    }

    @Nested
    class FinishMatch {

        private final MatchSummary startedMatch =
                scoreboard.startMatch("Mexico", "Canada");

        @Test
        void returnsFinalMatchResult() {
            scoreboard.updateScore(startedMatch.id(), 2, 1);

            MatchSummary finished = scoreboard.finishMatch(startedMatch.id());

            assertEquals(startedMatch.id(), finished.id());
            assertEquals("Mexico", finished.homeTeam());
            assertEquals("Canada", finished.awayTeam());
            assertEquals(2, finished.homeScore());
            assertEquals(1, finished.awayScore());
        }

        @Test
        void removesFinishedMatchFromSummary() {
            scoreboard.finishMatch(startedMatch.id());

            assertTrue(scoreboard.getSummary().isEmpty());
        }

        @Test
        void rejectsFinishingSameMatchTwice() {
            scoreboard.finishMatch(startedMatch.id());

            assertThrows(
                    MatchNotFoundException.class,
                    () -> scoreboard.finishMatch(startedMatch.id())
            );
        }

        @Test
        void rejectsUpdatingFinishedMatch() {
            scoreboard.finishMatch(startedMatch.id());

            assertThrows(
                    MatchNotFoundException.class,
                    () -> scoreboard.updateScore(startedMatch.id(), 1, 0)
            );
        }

        @Test
        void updatingFinishedMatchDoesNotReAddItToSummary() {
            scoreboard.finishMatch(startedMatch.id());

            assertThrows(
                    MatchNotFoundException.class,
                    () -> scoreboard.updateScore(startedMatch.id(), 1, 0)
            );

            assertTrue(scoreboard.getSummary().isEmpty());
        }

        @Test
        void rejectsNullMatchId() {
            assertThrows(
                    NullPointerException.class,
                    () -> scoreboard.finishMatch(null)
            );
        }

        @Test
        void rejectsUnknownMatchId() {
            MatchId unknownId = new MatchId(java.util.UUID.randomUUID());

            assertThrows(
                    MatchNotFoundException.class,
                    () -> scoreboard.finishMatch(unknownId)
            );
        }

        @Test
        void finishingMatchDoesNotAffectOtherMatches() {
            MatchSummary anotherMatch =
                    scoreboard.startMatch("Brazil", "Argentina");

            scoreboard.finishMatch(startedMatch.id());

            assertEquals(List.of(anotherMatch), scoreboard.getSummary());
        }

        @Test
        void allowsStartingSameTeamsAfterPreviousMatchFinished() {
            scoreboard.finishMatch(startedMatch.id());

            MatchSummary newMatch = scoreboard.startMatch("Mexico", "Canada");

            assertNotEquals(startedMatch.id(), newMatch.id());
            assertEquals(List.of(newMatch), scoreboard.getSummary());
        }
    }

    @Nested
    class GetSummary {

        @Test
        void returnsEmptyListWhenThereAreNoMatches() {
            assertTrue(scoreboard.getSummary().isEmpty());
        }

        @Test
        void sortsMatchesByTotalScoreDescending() {
            MatchSummary first = scoreboard.startMatch("Mexico", "Canada");
            MatchSummary second = scoreboard.startMatch("Spain", "Brazil");
            MatchSummary third = scoreboard.startMatch("Germany", "France");

            scoreboard.updateScore(first.id(), 0, 5);   // total 5
            scoreboard.updateScore(second.id(), 1, 2);  // total 3
            scoreboard.updateScore(third.id(), 3, 1);   // total 4

            assertEquals(
                    List.of(first.id(), third.id(), second.id()),
                    scoreboard.getSummary().stream().map(MatchSummary::id).toList()
            );
        }

        @Test
        void sortsEqualTotalsByMostRecentlyStartedMatchFirst() {
            MatchSummary first = scoreboard.startMatch("Mexico", "Canada");
            MatchSummary second = scoreboard.startMatch("Spain", "Brazil");
            MatchSummary third = scoreboard.startMatch("Germany", "France");

            scoreboard.updateScore(first.id(), 2, 1);
            scoreboard.updateScore(second.id(), 1, 2);
            scoreboard.updateScore(third.id(), 3, 0);

            assertEquals(
                    List.of(third.id(), second.id(), first.id()),
                    scoreboard.getSummary().stream()
                            .map(MatchSummary::id)
                            .toList()
            );
        }

        @Test
        void usesStartOrderRatherThanUpdateOrderAsTieBreaker() {
            MatchSummary first = scoreboard.startMatch("Mexico", "Canada");
            MatchSummary second = scoreboard.startMatch("Spain", "Brazil");

            scoreboard.updateScore(second.id(), 1, 1);
            scoreboard.updateScore(first.id(), 1, 1);

            assertEquals(
                    List.of(second.id(), first.id()),
                    scoreboard.getSummary().stream()
                            .map(MatchSummary::id)
                            .toList()
            );
        }

        @Test
        void sortsEqualTotalsWithDifferentScoreDistributions() {
            MatchSummary first = scoreboard.startMatch("Mexico", "Canada");
            MatchSummary second = scoreboard.startMatch("Spain", "Brazil");

            scoreboard.updateScore(first.id(), 3, 1);
            scoreboard.updateScore(second.id(), 2, 2);

            assertEquals(
                    List.of(second.id(), first.id()),
                    scoreboard.getSummary().stream()
                            .map(MatchSummary::id)
                            .toList()
            );
        }

        @Test
        void sortsZeroZeroMatchesByMostRecentlyStartedFirst() {
            MatchSummary first = scoreboard.startMatch("Mexico", "Canada");
            MatchSummary second = scoreboard.startMatch("Spain", "Brazil");

            assertEquals(
                    List.of(second.id(), first.id()),
                    scoreboard.getSummary().stream()
                            .map(MatchSummary::id)
                            .toList()
            );
        }

        @Test
        void reordersMatchesWhenScoreChanges() {
            MatchSummary first = scoreboard.startMatch("Mexico", "Canada");
            MatchSummary second = scoreboard.startMatch("Spain", "Brazil");

            scoreboard.updateScore(first.id(), 3, 0);
            scoreboard.updateScore(second.id(), 1, 1);

            assertEquals(
                    List.of(first.id(), second.id()),
                    scoreboard.getSummary().stream()
                            .map(MatchSummary::id)
                            .toList()
            );

            scoreboard.updateScore(second.id(), 4, 0);

            assertEquals(
                    List.of(second.id(), first.id()),
                    scoreboard.getSummary().stream()
                            .map(MatchSummary::id)
                            .toList()
            );
        }

        @Test
        void ordersManyMatchesWithEqualScoresByReverseStartOrder() {
            List<MatchId> ids = new ArrayList<>();

            for (int i = 0; i < 50; i++) {
                MatchSummary match =
                        scoreboard.startMatch("Home " + i, "Away " + i);
                scoreboard.updateScore(match.id(), 1, 1);
                ids.add(match.id());
            }

            List<MatchId> expected = new ArrayList<>(ids.reversed());

            assertEquals(
                    expected,
                    scoreboard.getSummary().stream()
                            .map(MatchSummary::id)
                            .toList()
            );
        }

        @Test
        void handlesTotalScoreOverflow() {
            MatchSummary first = scoreboard.startMatch("Mexico", "Canada");
            MatchSummary second = scoreboard.startMatch("Spain", "Brazil");

            scoreboard.updateScore(
                    first.id(), Integer.MAX_VALUE, Integer.MAX_VALUE
            );
            scoreboard.updateScore(second.id(), Integer.MAX_VALUE, 0);

            assertEquals(
                    List.of(first.id(), second.id()),
                    scoreboard.getSummary().stream()
                            .map(MatchSummary::id)
                            .toList()
            );
        }

        @Test
        void excludesFinishedMatches() {
            MatchSummary first = scoreboard.startMatch("Mexico", "Canada");
            MatchSummary second = scoreboard.startMatch("Spain", "Brazil");

            scoreboard.finishMatch(first.id());

            assertEquals(List.of(second), scoreboard.getSummary());
        }

        @Test
        void returnsEmptySummaryAfterAllMatchesAreFinished() {
            MatchSummary first = scoreboard.startMatch("Mexico", "Canada");
            MatchSummary second = scoreboard.startMatch("Spain", "Brazil");

            scoreboard.finishMatch(first.id());
            scoreboard.finishMatch(second.id());

            assertTrue(scoreboard.getSummary().isEmpty());
        }

        @Test
        void matchesReferenceExampleOrdering() {
            MatchSummary argentina = scoreboard.startMatch("Argentina", "Australia");
            MatchSummary uruguay = scoreboard.startMatch("Uruguay", "Italy");
            MatchSummary germany = scoreboard.startMatch("Germany", "Japan");
            MatchSummary spain = scoreboard.startMatch("Spain", "Brazil");

            scoreboard.updateScore(argentina.id(), 2, 2);
            scoreboard.updateScore(uruguay.id(), 6, 6);
            scoreboard.updateScore(germany.id(), 2, 2);
            scoreboard.updateScore(spain.id(), 4, 8);

            assertEquals(
                    List.of(spain.id(), uruguay.id(), germany.id(), argentina.id()),
                    scoreboard.getSummary().stream().map(MatchSummary::id).toList());
        }
    }

    @Nested
    class Immutability {

        @Test
        void summaryListIsUnmodifiableWhenNotEmpty() {
            scoreboard.startMatch("Mexico", "Canada");
            List<MatchSummary> summary = scoreboard.getSummary();
            MatchSummary foreign = new Scoreboard().startMatch("Spain", "Brazil");

            assertThrows(UnsupportedOperationException.class, () -> summary.add(foreign));
            assertThrows(UnsupportedOperationException.class, () -> summary.set(0, foreign));
            assertThrows(UnsupportedOperationException.class, () -> summary.remove(0));
            assertThrows(UnsupportedOperationException.class, summary::clear);
        }

        @Test
        void emptySummaryListIsUnmodifiable() {
            List<MatchSummary> summary = scoreboard.getSummary();
            MatchSummary foreign = new Scoreboard().startMatch("Spain", "Brazil");

            assertThrows(UnsupportedOperationException.class, () -> summary.add(foreign));
        }

        @Test
        void previousSnapshotIsUnaffectedByScoreUpdate() {
            MatchSummary match = scoreboard.startMatch("Mexico", "Canada");
            List<MatchSummary> snapshot = scoreboard.getSummary();

            scoreboard.updateScore(match.id(), 2, 1);

            assertEquals(0, snapshot.getFirst().homeScore());
            assertEquals(0, snapshot.getFirst().awayScore());
        }

        @Test
        void previousSnapshotIsUnaffectedByStartingAnotherMatch() {
            MatchSummary match = scoreboard.startMatch("Mexico", "Canada");
            List<MatchSummary> snapshot = scoreboard.getSummary();

            scoreboard.startMatch("Spain", "Brazil");

            assertEquals(1, snapshot.size());
            assertEquals(match.id(), snapshot.getFirst().id());
        }

        @Test
        void previousSnapshotIsUnaffectedByFinishingMatch() {
            MatchSummary match = scoreboard.startMatch("Mexico", "Canada");
            List<MatchSummary> snapshot = scoreboard.getSummary();

            scoreboard.finishMatch(match.id());

            assertEquals(1, snapshot.size());
            assertEquals(match.id(), snapshot.getFirst().id());
        }

        @Test
        void returnedMatchSummaryIsUnaffectedByLaterUpdates() {
            MatchSummary match = scoreboard.startMatch("Mexico", "Canada");

            MatchSummary original = scoreboard.updateScore(match.id(), 2, 1);

            scoreboard.updateScore(match.id(), 5, 3);

            assertEquals(2, original.homeScore());
            assertEquals(1, original.awayScore());
        }
    }

    @Nested
    class Exceptions {

        @Test
        void allDomainExceptionsAreUncheckedAndExtendScoreboardException() {
            assertTrue(RuntimeException.class.isAssignableFrom(ScoreboardException.class));
            assertTrue(ScoreboardException.class.isAssignableFrom(MatchNotFoundException.class));
            assertTrue(ScoreboardException.class.isAssignableFrom(InvalidTeamException.class));
            assertTrue(ScoreboardException.class.isAssignableFrom(InvalidScoreException.class));
        }
    }
}
