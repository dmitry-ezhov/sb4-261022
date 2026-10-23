package com.example.library;

import java.time.Clock;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.example.library.support.TestClocks;

/**
 * The clock the tests run on.
 *
 * <p>The application injects its clock rather than calling
 * {@code LocalDate.now()}, so a test can decide what today is. The fixture
 * comes from the shared support module, because it is not about the domain:
 * every lab carries its own copy of the domain classes.
 */
@TestConfiguration
public class FixedClockConfiguration {

    @Bean
    @Primary
    Clock fixedClock() {
        return TestClocks.fixed();
    }
}
