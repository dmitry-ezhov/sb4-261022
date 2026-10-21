package com.example.library.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class TestClocksTest {

    @Test
    void fixedClockDoesNotAdvance() throws InterruptedException {
        Clock clock = TestClocks.fixed();

        Instant first = clock.instant();
        Thread.sleep(2);
        Instant second = clock.instant();

        assertThat(second).isEqualTo(first);
    }

    @Test
    void fixedClockIsInUtc() {
        assertThat(TestClocks.fixed().getZone()).isEqualTo(ZoneOffset.UTC);
    }
}
