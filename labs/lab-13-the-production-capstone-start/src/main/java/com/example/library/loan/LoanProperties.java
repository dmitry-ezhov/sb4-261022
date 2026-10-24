package com.example.library.loan;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The library's lending rules, as configuration.
 *
 * @param period how long a loan lasts
 * @param limit how many loans a member may hold at once
 */
@Validated
@ConfigurationProperties(prefix = "library.loan")
public record LoanProperties(
        @NotNull Duration period, @Min(1) int limit) {}
