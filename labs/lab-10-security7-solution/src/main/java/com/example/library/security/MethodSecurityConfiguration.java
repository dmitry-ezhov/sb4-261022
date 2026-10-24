package com.example.library.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Method security, switched on explicitly - Boot auto-configures chains, but
 * it does not enable this, and without it the {@code @PreAuthorize} on
 * {@code LoanService#borrow} quietly annotates nothing.
 *
 * <p>The annotation is the one whose defaults run something.
 * {@code @EnableMethodSecurity} in 7.1.1 defaults
 * {@code prePostEnabled = true}, so {@code @PreAuthorize} is live with no
 * attribute at all. The old {@code @EnableGlobalMethodSecurity} is still in
 * the jar and still defaults {@code prePostEnabled = false} - the
 * attribute-less form of the old annotation switched on exactly nothing,
 * which is the quietest upgrade defect in this area. There is no
 * {@code prePostEnabled} attribute written here, because its default is
 * already the value this course means.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class MethodSecurityConfiguration {}
