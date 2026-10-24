package com.example.library.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * The principals the chains authenticate, in a store the application owns.
 *
 * <p>Boot would otherwise generate one principal in memory and print its
 * password at boot; the library needs its Members and the management
 * surface's Operator, with names a reference implementation can be written
 * against: the Members {@code ada}, {@code bruno} and {@code cecil}, each
 * the {@code MEMBER} role the lending endpoints require, and the Operator
 * {@code operator}, the {@code OPERATOR} role the management chain requires
 * of its callers. A Member's authenticated name is what a Loan row carries
 * as its {@code member_id} - the name is the identity the borrow is issued
 * in, not a field the request body chose.
 *
 * <p>The credentials live here, one store of the lab's principals; a
 * deployment waters this by pointing {@code UserDetailsService} at whatever
 * its provisioning writes. What does not change with such a swap is the shape
 * of a stored password: every stored value here is produced by the
 * {@link PasswordEncoder} this class declares, so every value names its
 * algorithm through the delegating factory's {@code {bcrypt}} prefix, and a
 * row's encoder can change without the comparison learning a constant. That
 * is also why no plaintext sits in a property or a migration, and why the
 * generated development principal Boot would otherwise print leaves no trace
 * here.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityUsersConfiguration {

    private static final String MEMBER_PASSWORD = "member-password";

    private static final String OPERATOR_PASSWORD = "operator-password";

    /**
     * The delegating encoder: it writes the algorithm id into every stored
     * value - {@code {bcrypt}$2a$...} today - and reads the id back to pick
     * the comparison, so an algorithm migration is a re-encode of rows
     * rather than a rewrite of the configuration.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService libraryPrincipals(PasswordEncoder passwordEncoder) {
        return new InMemoryUserDetailsManager(List.of(
                member("ada", passwordEncoder),
                member("bruno", passwordEncoder),
                member("cecil", passwordEncoder),
                operator(passwordEncoder)));
    }

    /**
     * One Member at the {@code MEMBER} role, in the words the domain speaks:
     * the person who borrows, stored with a password the encoder produced -
     * not a plaintext property line, and not a hashed row someone copied
     * without its prefix.
     */
    private static UserDetails member(String name, PasswordEncoder passwordEncoder) {
        return principal(name, MEMBER_PASSWORD)
                .roles("MEMBER")
                .passwordEncoder(passwordEncoder::encode)
                .build();
    }

    /**
     * The management surface's Operator, at the {@code OPERATOR} role whose
     * name the management chain's rule spells - the role the routes a
     * monitor's caller holds, stored next to the Members rather than instead
     * of them.
     */
    private static UserDetails operator(PasswordEncoder passwordEncoder) {
        return principal("operator", OPERATOR_PASSWORD)
                .roles("OPERATOR")
                .passwordEncoder(passwordEncoder::encode)
                .build();
    }

    private static User.UserBuilder principal(String name, String password) {
        return User.withUsername(name).password(password);
    }
}
