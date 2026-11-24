package com.sportradar.worldcup.scoreboard.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvalidMatchIdExceptionTest {

    @Test
    void messageConstructorPreservesMessage() {
        InvalidMatchIdException exception = new InvalidMatchIdException("invalid id");

        assertThat(exception).hasMessage("invalid id");
        assertThat(exception).hasNoCause();
    }
}
