package com.example.library.availability;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where the availability service lives.
 *
 * @param baseUrl the service's base URL
 */
@ConfigurationProperties(prefix = "library.availability")
public record AvailabilityProperties(String baseUrl) {}
