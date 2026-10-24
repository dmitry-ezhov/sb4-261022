package com.example.library.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * The library's two chains, and how they are written in Security 7: there is
 * no subclass and no {@code configure} override to write - a chain is a
 * {@code SecurityFilterChain} bean that takes the {@code HttpSecurity} the
 * container hands it and returns what {@code build()} made of it.
 *
 * <p>The lambda is the only form left. {@code authorizeRequests()} and every
 * {@code and()} are gone from 7.1.1 - deleted, not deprecated - and the
 * registries' own methods chain, which is why the rules below are one chain
 * of calls with no {@code and()} in sight. {@code csrf(...)},
 * {@code httpBasic(...)} and every other configurer are
 * {@code Customizer}-only: the no-arg overloads left with them. The only
 * matcher class named is {@link PathPatternRequestMatcher} -
 * {@code AntPathRequestMatcher} and {@code MvcRequestMatcher} are not in the
 * 7.1.1 jar at all - and the one {@code securityMatcher} that needs an
 * explicit matcher builds it with {@code PathPatternRequestMatcher
 * .withDefaults()}.
 *
 * <p>There are two chains because there are two surfaces with two answers to
 * &ldquo;who may ask&rdquo;. Order is explicit, and it is the whole wiring of
 * the two: Security walks the chains in order and uses the first whose
 * matcher accepts the request, and the no-matcher API chain accepts
 * everything - so the management chain is {@code @Order(1)} and the API chain
 * {@code @Order(2)}. Without the ordering the no-matcher chain would win
 * both, and the management chain would be a bean nobody reads.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfiguration {

    /**
     * The management surface's chain, first in the walk: it claims
     * {@code /actuator/**} and owns it, on the management socket M09
     * separated and now guarded here. The M09 health probe stays public - it
     * is the one read a monitor must not need a credential for - and every
     * other endpoint behind authentication requires the {@code OPERATOR}
     * role, the role the management surface's own callers hold. The API
     * chain gets no actuator rule of its own: once this chain exists it owns
     * {@code /actuator/**} on every socket, and a second rule for the same
     * paths would be a rule nobody could tell apart.
     */
    @Bean
    @Order(1)
    SecurityFilterChain managementSurface(HttpSecurity http) {
        http.securityMatcher(PathPatternRequestMatcher.withDefaults().matcher("/actuator/**"))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health/**")
                        .permitAll()
                        .anyRequest()
                        .hasRole("OPERATOR"))
                .httpBasic(Customizer.withDefaults())
                .csrf(CsrfConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    /**
     * The public API's chain, last in the walk - it has no
     * {@code securityMatcher}, so it accepts everything the management chain
     * did not. Its ordered rules are the library's answer to who may ask: the
     * Title catalogue's reads are public first, the lending endpoints
     * require the {@code MEMBER} role next, and {@code anyRequest()
     * .authenticated()} closes the chain - the Reservation surface and every
     * path the rules do not name are closed by that last rule, not by being
     * named.
     *
     * <p>Stateless, by the same argument that disables the CSRF protection:
     * the chain authenticates with HTTP Basic, whose credential the client's
     * code attaches on every request - there is no session to hold, no
     * cookie a browser would attach for it, and therefore no ambient
     * credential a cross-site page could borrow. {@code STATELESS} and
     * {@code csrf.disable()} are one argument, not two settings: a chain
     * that disables CSRF while keeping a session is a defect.
     */
    @Bean
    @Order(2)
    SecurityFilterChain libraryApi(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.GET, "/api/titles/**")
                        .permitAll()
                        .requestMatchers("/api/loans/**")
                        .hasRole("MEMBER")
                        .anyRequest()
                        .authenticated())
                .httpBasic(Customizer.withDefaults())
                .csrf(CsrfConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }
}
