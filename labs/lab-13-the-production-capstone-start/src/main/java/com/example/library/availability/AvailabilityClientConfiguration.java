package com.example.library.availability;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * The availability client, built from the interface.
 *
 * <p>The interface says what the service offers; the proxy says how to call
 * it, and turns a 4xx or 5xx into a typed exception rather than a status
 * code every caller has to check.
 */
@Configuration
public class AvailabilityClientConfiguration {

    @Bean
    public AvailabilityClient availabilityClient(RestClient.Builder builder, AvailabilityProperties properties) {
        RestClient client = builder.baseUrl(properties.baseUrl()).build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(client))
                .build();
        return factory.createClient(AvailabilityClient.class);
    }
}
