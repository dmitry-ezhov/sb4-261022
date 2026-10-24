package com.example.library.availability;

import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

/**
 * The availability feature's resilient read: the client behind a replay
 * policy.
 *
 * <p>The borrow asks this checker, not the client directly, and the call
 * enters it from outside its bean - the controller holds the checker the
 * container built, so every ask crosses the proxy the resilience
 * machinery wrapped the checker in, where the retry rides. A call made from
 * inside the class - {@code this.availability(...)} - never crosses the
 * proxy, and the annotation below would be dead source.
 */
@Service
public class AvailabilityChecker {

    private final AvailabilityClient client;

    AvailabilityChecker(AvailabilityClient client) {
        this.client = client;
    }

    /**
     * Asks whether a copy is on the shelf, replaying the wire failures a
     * service that blips produces.
     *
     * <p>The annotation's geometry, each value a sentence and none of them
     * the annotation's defaults: three attempts in total ({@code
     * maxRetries} counts retries, not attempts), because the availability
     * service blips in fractions of a second and a fourth ask is an outage,
     * not a blip; a 100ms base delay ({@code delay}) so the first replay
     * lands inside the blip; 50ms of {@code jitter} so replays from
     * simultaneous borrows do not arrive in lockstep and amplify the very
     * burst they back off; a {@code multiplier} of 2, growing each replay's
     * delay - 100ms, then 200ms - capped by a {@code maxDelay} of a second
     * this schedule never reaches; and {@code includes} naming the one
     * failure the replay may carry - {@link ResourceAccessException}, the
     * wire failure M04's contract already spoke of could-not-connect and
     * connection-dropped, matched also through the thrown exception's
     * nested causes.
     *
     * <p>When the budget is spent, the framework's retry interceptor
     * rethrows the last attempt's failure - the wire failure itself, not a
     * wrapping exception - and the boundary answers it in the RFC 9457
     * words the advice owns.
     *
     * @param copyId the copy to ask about
     * @return what the service knows about the copy, once an attempt
     *         succeeded
     * @throws ResourceAccessException when every attempt failed on the wire
     *         - the spent budget's last failure, answered by the advice
     */
    @Retryable(
            includes = ResourceAccessException.class,
            maxRetries = 2,
            delay = 100,
            jitter = 50,
            multiplier = 2,
            maxDelay = 1000)
    public Availability availability(String copyId) {
        return this.client.availability(copyId);
    }
}
