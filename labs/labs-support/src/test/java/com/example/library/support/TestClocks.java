package com.example.library.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Clocks with a fixed instant, for labs that need a deterministic "now".
 *
 * <p>A lab that reads the clock - about due dates, loan periods, anything with
 * a deadline - gets the same answer on every machine and on every run, so that
 * its tests assert behaviour rather than timing.
 */
public final class TestClocks {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-05T09:00:00Z");

    private TestClocks() {}

    /**
     * A clock frozen at a fixed instant, in UTC.
     *
     * @return a clock that always reports the same instant
     */
    public static Clock fixed() {
        return Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    }
}
