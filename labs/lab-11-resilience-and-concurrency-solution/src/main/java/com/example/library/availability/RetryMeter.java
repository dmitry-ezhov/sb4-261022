package com.example.library.availability;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.context.event.EventListener;
import org.springframework.resilience.retry.MethodRetryEvent;
import org.springframework.stereotype.Component;

/**
 * The replay's repetitions, made observable - a retry without an observer
 * is a deficit nobody owns.
 *
 * <p>Framework 7 wires the observer into the event mechanism the
 * application already has: every exception a retry processing encounters
 * publishes a {@link MethodRetryEvent} - it extends
 * {@code MethodFailureEvent}, which extends {@code ApplicationEvent} - and
 * the M09 listening shape is all it takes. One listener serves every
 * resilient method in the application; the counter is labelled at least by
 * the annotated method's name, so {@code library.availability.retried} on
 * the dashboard answers next to M09's {@code library.loans.issued} the
 * question only it can: how many times the library asked again.
 */
@Component
public class RetryMeter {

    /** The counter's name, in the vocabulary the availability feature speaks. */
    static final String RETRIED = "library.availability.retried";

    private final MeterRegistry registry;

    RetryMeter(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Counts one replayed attempt, labelled by the method the policy rides.
     *
     * @param event the replay event the framework published for one
     *        exception the retry processing encountered
     */
    @EventListener
    void onMethodRetryEvent(MethodRetryEvent event) {
        this.registry.counter(RETRIED, "method", event.getMethod().getName()).increment();
    }
}
