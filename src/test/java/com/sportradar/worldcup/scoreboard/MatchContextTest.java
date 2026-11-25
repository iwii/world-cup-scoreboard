package com.sportradar.worldcup.scoreboard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchContextTest {

    @Test
    @DisplayName("Lifecycle methods require the match lock")
    void lifecycleMethodsRequireTheMatchLock() {
        MatchContext context = new MatchContext(
                MatchId.random(), "Mexico", "Canada", 1, MatchStatus.IN_PROGRESS);

        assertThatThrownBy(context::isInProgressUnderLocked)
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(context::markFinishedUnderLock)
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @DisplayName("Constructor rejects a null status")
    void constructorRejectsNullStatus() {
        assertThatThrownBy(() -> new MatchContext(
                MatchId.random(), "Mexico", "Canada", 1, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("status");
    }

    @Test
    @DisplayName("Match lifecycle transitions from in progress to finished")
    void matchLifecycleTransitionsFromInProgressToFinished() {
        MatchContext context = new MatchContext(
                MatchId.random(), "Mexico", "Canada", 1, MatchStatus.IN_PROGRESS);

        context.lock.lock();
        try {
            assertThat(context.isInProgressUnderLocked()).isTrue();
            assertThat(context.toSnapshotIfActive()).isPresent();

            context.markFinishedUnderLock();

            assertThat(context.isInProgressUnderLocked()).isFalse();
            assertThat(context.toSnapshotIfActive()).isEmpty();
        } finally {
            context.lock.unlock();
        }
    }
}
