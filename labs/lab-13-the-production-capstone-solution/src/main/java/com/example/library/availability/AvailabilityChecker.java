package com.example.library.availability;

import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

/**
 * The availability feature's resilient read: the client behind a replay
 * policy, and the read whose landing the process tells the platform about.
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
    private final ApplicationEventPublisher publisher;

    AvailabilityChecker(AvailabilityClient client, ApplicationEventPublisher publisher) {
        this.client = client;
        this.publisher = publisher;
    }

    /**
     * Asks whether a Copy is on the shelf, replaying the wire failures a
     * service that blips produces.
     *
     * <p>The geometry is the environment's, not the annotation's constant:
     * every number arrives through the annotation's String variants, which
     * carry {@code ${...}} placeholders, beside the base URL the module
     * already binds - one replay geometry, bound as configuration
     * properties, kept in the {@code application.yml} where a deployment can
     * see and name it, and reached the same way from a command line that
     * outranks the jar's own yml. The placeholder's default is the jar's own
     * geometry, so a boot whose environment names none keeps exactly the
     * policy that was reviewed: two retries for three attempts in total
     * ({@code max-retries} counts retries, not attempts - a fourth ask is an
     * outage, not a blip), a 100ms base delay so the first replay lands
     * inside the blip, 50ms of jitter so replays from simultaneous borrows
     * do not arrive in lockstep and amplify the very burst they back off, a
     * multiplier of 2 growing each replay's delay - 100ms, then 200ms -
     * capped by a max delay of a second this schedule never reaches. The
     * freshly booted process reads its environment once, at proxy build
     * time, and a deployment's override needs no rebuild, where the
     * annotation's own {@code maxRetries} numbers would have been compiled
     * decisions no property source could reach.
     *
     * <p>{@code includes} names the one failure the replay may carry -
     * {@link ResourceAccessException}, the wire failure M04's contract
     * already spoke of could-not-connect and connection-dropped, matched
     * also through the thrown exception's nested causes. When the budget is
     * spent, the framework's retry interceptor rethrows the last attempt's
     * failure - the wire failure itself, not a wrapping exception - and the
     * boundary answers it in the RFC 9457 words the advice owns, where the
     * publish that moves readiness lives.
     *
     * @param copyId the copy to ask about
     * @return what the service knows about the copy, once an attempt
     *         succeeded
     * @throws ResourceAccessException when every attempt failed on the wire
     *         - the spent budget's last failure, answered - and published -
     *         by the advice
     */
    @Retryable(
            includes = ResourceAccessException.class,
            maxRetriesString = "${library.availability.max-retries:2}",
            delayString = "${library.availability.delay:100}",
            jitterString = "${library.availability.jitter:50}",
            multiplierString = "${library.availability.multiplier:2}",
            maxDelayString = "${library.availability.max-delay:1000}")
    public Availability availability(String copyId) {
        Availability answered = this.client.availability(copyId);
        // The read landed: the process trades again. Published, in the
        // checker, on every read that answers - the recovery half of the
        // outage's story, readiness moving back the same way it moved out.
        AvailabilityChangeEvent.publish(this.publisher, this, ReadinessState.ACCEPTING_TRAFFIC);
        return answered;
    }
}
