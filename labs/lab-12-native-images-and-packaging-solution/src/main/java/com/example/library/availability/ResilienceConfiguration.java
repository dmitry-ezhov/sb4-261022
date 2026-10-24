package com.example.library.availability;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

/**
 * The resilience machinery's switch, in one place, on no code of its own.
 *
 * <p>Framework 7 answers the retry question the Boot 2 era answered with
 * spring-retry or a Resilience4j starter: the annotations and their
 * post-processing live in {@code org.springframework.resilience.annotation},
 * inside the spring-context jar this build already resolves. This class is
 * the one {@code @EnableResilientMethods} - annotated methods begin retrying
 * when their call crosses their bean's proxy, and never when a turn calls
 * itself - and no second copy of the policy rides the classpath: no
 * spring-retry artifact (whose {@code @Retryable} shares the name, not the
 * meaning), no Resilience4j starter, no hand-rolled loop around a
 * sleep.
 */
@Configuration
@EnableResilientMethods
public class ResilienceConfiguration {}
