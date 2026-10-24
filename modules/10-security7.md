# M10 - Spring Security 7

M09 closed with "who is allowed to ask" and handed the question to this Module.
The library's API is still the one M08 built: `POST /api/loans` trusts the
`memberId` in the body, the catalogue is readable by anyone who can reach the
port, and the management surface M09 separated is protected by nothing but its
own obscurity.
This Module is the answer: a [Member](../GLOSSARY.md) authenticates, the
lending endpoints require that authentication, and the [Loan limit](../GLOSSARY.md)
is counted against the authenticated Member rather than against whatever id
the request supplied. By the end of it you know why the chain is a bean rather
than a subclass, why the lambda (or `Customizer`) is the only form Security
7.1.1 still accepts, what method security's expression can and cannot protect,
what CSRF actually protects and which release changed its defaults (the answer
is not Boot 4), how passwords are stored so the hash names its own algorithm,
and how a resource server differs from a session.

## What you knew

In Boot 2, security was one starter and one subclass.
`spring-boot-starter-security` on the classpath secured every endpoint,
printed a generated password at boot and answered HTTP Basic; to change that
you wrote `@EnableWebSecurity`, extended `WebSecurityConfigurerAdapter`, and
overrode `configure(HttpSecurity http)` with the chain you had copied since
Boot 1: `authorizeRequests()`, `antMatchers(...)`,
`anyRequest().authenticated()`, `and().formLogin().and().httpBasic()`. The
`and()` was not decoration - each configurer returned itself, and `and()`
climbed back to the `HttpSecurity` - and the adapter was already `@Deprecated`
in 5.7, a fact most Boot 2 codebases ignored because the old form kept
compiling.

The rest of the Boot 2 picture: CSRF was on by default, with a token in the
session and a `_csrf` parameter or header; method security was
`@EnableGlobalMethodSecurity(prePostEnabled = true)` over `@PreAuthorize`,
whose `prePostEnabled` default was **false** - omit the attribute and the
annotations were silently inert. Passwords went through a `PasswordEncoder`
bean, usually `BCryptPasswordEncoder`, or
`PasswordEncoderFactories.createDelegatingPasswordEncoder()` when you wanted
the stored hash to name its own algorithm with a `{bcrypt}` prefix. A resource
server was `spring-boot-starter-oauth2-resource-server` and
`http.oauth2ResourceServer().jwt()`; a security test was `spring-security-test`
and `@WithMockUser`. Those are the six habits this Module re-reads against
7.1.1.

You are assumed to know all of that fluently. You are **not** assumed to know
that `WebSecurityConfigurerAdapter` left in Security 6.0, that
`authorizeRequests()`, `and()` and every no-arg configurer method left in
Security 7.0, that `AntPathRequestMatcher` and `MvcRequestMatcher` are not in
the 7.1.1 jar at all, that `@EnableMethodSecurity` is the annotation to use
and `@EnableGlobalMethodSecurity` is present-but-deprecated rather than
removed, that the CSRF defaults changed in Security 5.8, survived 7.0 intact,
and were **not** touched at Boot 4's boundary, that Boot 4 moved the security
auto-configuration into a `spring-boot-security` module with a new package
root, or that `@WithMockUser` now arrives through a starter of its own.

## What changed

The Boot 4 column is the one this course's build resolves - Spring Boot 4.1.1
over Spring Security 7.1.1 - every cell read off the published
`spring-boot-dependencies` POMs of 2.7.18, 3.5.16 and 4.1.1, off the
`spring-security-config`, `spring-security-web`, `spring-security-core` and
`spring-boot-security` jars the build resolves (`javap` for the signatures and
the annotation defaults, `unzip -l` for what is present at all), and - where a
*behaviour* had to be seen, not stated - off a 4.1.1 application booting in
front of us (see the verified-versions table in the
[lab index](../labs/README.md)). The Boot 2 column is the last Boot 2 (2.7),
the Boot 3 column the last Boot 3 (3.5); the security versions resolved there
are **5.7.11** and **6.5.11**.

|  | Spring Boot 2 (2.7) | Spring Boot 3 (3.5) | Spring Boot 4 (4.1.1) |
|---|---|---|---|
| The security starter | `spring-boot-starter-security` pulls `spring-security-config` + `spring-security-web` **5.7.11**, and Boot's auto-configuration still lives inside `spring-boot-autoconfigure` | the same starter shape over `spring-security-*` **6.5.11** | the same starter name, whose 4.1.1 POM now declares the Boot-owned `spring-boot-security` module plus `spring-aop`, over `spring-security-*` **7.1.1** - the packaging row below is what changed, not the name |
| Where the chain is declared | `@EnableWebSecurity` + a `WebSecurityConfigurerAdapter` subclass overriding `configure(HttpSecurity)`; the adapter is `@Deprecated` in 5.7 | a `@Bean SecurityFilterChain` taking `HttpSecurity`; `WebSecurityConfigurerAdapter` is **absent** from 6.5.11 - it was removed in 6.0 | the same bean; the class is still absent from 7.1.1 |
| How the chain is written | `authorizeRequests()`, `antMatchers(...)` / `mvcMatchers(...)`, `anyRequest()`, `and()`; each configurer returns itself | `authorizeHttpRequests(auth -> ...)` + `requestMatchers(...)`; `authorizeRequests()` and `and()` still compile, but `antMatchers()` / `mvcMatchers()` are gone from 6.5.11 | `authorizeHttpRequests(Customizer)` only; `csrf`, `formLogin`, `httpBasic` and `oauth2ResourceServer` are `Customizer`-only, with no no-arg overload; `authorizeRequests()` and `and()` are gone - we listed every public method of 7.1.1's `HttpSecurity` |
| The request matcher | `AntPathRequestMatcher` and `MvcRequestMatcher` in `spring-security-web` 5.7.11 | both of those, plus `PathPatternRequestMatcher` (present in 6.5.11); `requestMatchers(String...)` chooses Ant or MVC by what is on the classpath | `PathPatternRequestMatcher` only: `AntPathRequestMatcher` and `MvcRequestMatcher` are not in the 7.1.1 jar, and the string overloads build a PathPattern matcher |
| Method security | `@EnableGlobalMethodSecurity(prePostEnabled = true)`; its `prePostEnabled` default is **false**; `@EnableMethodSecurity` already exists in 5.7.11 and already defaults `prePostEnabled=true` | `@EnableMethodSecurity` is the recommended annotation; `@EnableGlobalMethodSecurity` is `@Deprecated` in 6.5.11; neither is auto-configured by Boot | unchanged from 6.5: `@EnableMethodSecurity` defaults `prePostEnabled=true`, `securedEnabled=false`, `jsr250Enabled=false`, `mode=PROXY`; `@EnableGlobalMethodSecurity` is still present and still deprecated |
| CSRF defaults | on by default; `CsrfConfigurer` wraps `HttpSessionCsrfTokenRepository` in a `LazyCsrfTokenRepository`; `CsrfFilter` has no request-handler abstraction and no `DeferredCsrfToken` | on by default; the default repository is `HttpSessionCsrfTokenRepository`; `CsrfFilter` defaults to `XorCsrfTokenRequestAttributeHandler` and loads a `DeferredCsrfToken` | **unchanged from 6.5**: same repository, same XOR handler, same deferred token, same unsafe methods (everything except `GET`, `HEAD`, `TRACE`, `OPTIONS`); 7.0 adds `csrf(csrf -> csrf.spa())` and narrows `CookieCsrfTokenRepository` to `setCookieCustomizer` + name/path |
| Password encoding | `PasswordEncoderFactories.createDelegatingPasswordEncoder()`, `{id}`-prefixed, default `bcrypt`; `BCryptPasswordEncoder` available directly | the same factory and the same default | the same factory and the same default; 7.0 adds `Password4jPasswordEncoder` and its algorithm-specific encoders; Boot's default principal still gets `{noop}` when the configured password has no `{id}` - the constant is in the 2.7 and 4.1.1 bytecode alike |
| Resource server | `spring-boot-starter-oauth2-resource-server`; `http.oauth2ResourceServer().jwt()` | the same starter; `oauth2ResourceServer(oauth2 -> oauth2.jwt(...))`, with the no-arg overload still present | starter renamed `spring-boot-starter-security-oauth2-resource-server` (the old name is still published, and its 4.1.1 POM says deprecated in favor of the new one); `oauth2ResourceServer(Customizer)` only, and `jwt(Customizer)` / `opaqueToken(Customizer)` only |
| Where Boot's auto-configuration lives | `org.springframework.boot.autoconfigure.security.servlet.*` inside `spring-boot-autoconfigure` | the same package and the same jar | `org.springframework.boot.security.autoconfigure.*`, with the servlet chain in `.web.servlet`, inside the new `spring-boot-security` module the starter pulls |
| Security in tests | `spring-security-test` on the test classpath; `@WithMockUser` | the same | `spring-boot-starter-security-test`, which brings `spring-boot-security-test`; `spring-boot-starter-test` alone does not put `@WithMockUser` on the classpath |

Ten rows, and the sum is one sentence: **Security 7 is the release that
deleted what Security 6 had deprecated; Boot 4 is the release that moved the
packaging underneath it.** That sentence is the whole Module, and the rows
worth re-reading at full length are the ones that prove it.

- **The first break is 6.0, not 7.0.** A Boot 2 codebase that upgrades
  straight to 4.1.1 breaks twice, and the first break is the one the Boot 2
  memory is least prepared for: `WebSecurityConfigurerAdapter` is gone -
  deprecated in 5.7, removed in 6.0 - so the subclass has no superclass to
  extend. The replacement is not a new method on the adapter; it is a
  `SecurityFilterChain` bean, and that bean is what the rest of this Module
  writes. The 6.0 release also renamed the authorization DSL (the "How the
  chain is written" row), so the `configure` body a Boot 2 reader remembers is
  not the body that compiles either.
- **The second break is 7.0, and it is a deletion, not a redesign.**
  `authorizeRequests()` survived 6.5 as a deprecated method; 7.0 removed it.
  `and()` survived 6.5; 7.0 removed it. The no-arg `csrf()`, `formLogin()`,
  `httpBasic()` and `oauth2ResourceServer()` survived 6.5; 7.0 removed them,
  leaving only the `Customizer` overloads. `AntPathRequestMatcher` and
  `MvcRequestMatcher` survived 6.5; 7.0 removed both classes. This is why the
  delta table's Boot 3 column and Boot 4 column can look so different while
  the *behaviour* of the chain is the same: the behaviour never moved, the
  spelled-out API did.
- **Boot 4's own move is packaging.** The 4.1.1 starter's POM names
  `spring-boot-security`, a module 3.5.16 does not have; the auto-configuration
  left `org.springframework.boot.autoconfigure.security.servlet` for
  `org.springframework.boot.security.autoconfigure`, and the servlet chain
  sits in `...autoconfigure.web.servlet`. The resource-server starter was
  renamed in the same release, with the old name kept alive and marked
  deprecated. None of that changes an expression, a matcher or a token; all of
  it changes an import, a POM line or a starter name, which is exactly the
  kind of change an upgrade review has to read rather than remember.

The CSRF row needs its own paragraph, because the brief for this Module said
"the CSRF defaults Boot 4 changed" - and **Boot 4 changed none of them.** We
checked the claim the way M08 checked its disappearing
`JpaTransactionManagerAutoConfiguration`: bytecode first, then a running
application. In 4.1.1's `CsrfConfigurer`, the default repository is
still `HttpSessionCsrfTokenRepository`; in 4.1.1's `CsrfFilter`, the default
request handler is still `XorCsrfTokenRequestAttributeHandler` and the token
is still a `DeferredCsrfToken`; the set of methods that need a token is still
"everything except `GET`, `HEAD`, `TRACE`, `OPTIONS`"; and Boot 4's default
`SecurityFilterChain` still calls `authorizeHttpRequests(...)`, `formLogin(...)`
and `httpBasic(...)` without touching CSRF. The change the ticket remembered is
older than the ticket thought: the deferred token and the XOR handler arrived
in Security 5.8 (Boot 3's 6.5.11 inherits both, which is why the Boot 3 column
already shows them). The change that *is* new in 7.0 is additive:
`csrf.spa()` and the narrowed cookie repository. The table puts each change in
the column where it happened; the CSRF section below separates the releases so
the upgrade review can name the one it is actually paying for.

What moved since Security 5, in the order a Boot 2 codebase meets it:

1. **5.7 -> 6.0: the component model.** The adapter goes; the
   `SecurityFilterChain` bean replaces it; `authorizeRequests` and
   `antMatchers` begin their deprecation; `@EnableGlobalMethodSecurity` is
   deprecated in favor of `@EnableMethodSecurity`. This is the release that
   changes how the chain is *declared*. (CSRF's deferred token and XOR handler
   are older still - they arrived in Security 5.8, which Boot 2.7.18 does not
   resolve, so the Boot 2 column shows the pre-5.8 behaviour by version rather
   than by Boot generation.)
2. **6.x -> 7.0: the deletions.** The deprecated surface is removed:
   `authorizeRequests`, `and()`, the no-arg configurers, the Ant and MVC
   matcher classes. In the same release, `csrf.spa()` arrives and
   `CookieCsrfTokenRepository` trades four individual setters for one cookie
   customizer. This is the release that changes how the chain is *spelled*.
3. **3.5 -> 4.1.1: the packaging.** Security itself stays at 7.1.1's API;
   Boot moves the auto-configuration into `spring-boot-security`, renames the
   resource-server starter, and introduces `spring-boot-starter-security-test`.
   This is the release that changes where the pieces *live*.

## How it works now

### The chain is a bean, and the lambda is the form

A Boot 4 chain is a `SecurityFilterChain` bean. There is no subclass and no
`configure` override; the method takes the `HttpSecurity` the container hands
it and returns the built chain:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    @Order(2)
    SecurityFilterChain libraryApi(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/titles/**")
                        .permitAll()
                        .requestMatchers("/actuator/health/**")
                        .permitAll()
                        .requestMatchers("/api/loans/**")
                        .hasRole("MEMBER")
                        .anyRequest()
                        .authenticated())
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }
}
```

Every line is a 7.1.1 fact:

- `authorizeHttpRequests(Customizer)` is the only authorization entry point
  left. Its lambda receives the request-matcher registry, and every
  authorization rule is one chain of calls on that registry - no `and()`,
  because the registry's methods return the registry, and the configurers
  return `HttpSecurity`, which is what lets the outer chain continue.
- `requestMatchers(HttpMethod.GET, ...)` and `requestMatchers(String...)` still
  exist; under them, 7.1.1 builds a `PathPatternRequestMatcher`. When a chain
  has to be selected by path before authorization runs - the management
  surface, for instance - the matcher class is named directly, because
  `securityMatcher` takes a `RequestMatcher` and the Ant and MVC classes are
  not there to take:

  ```java
  @Bean
  @Order(1)
  SecurityFilterChain actuatorApi(HttpSecurity http) throws Exception {
      http.securityMatcher(PathPatternRequestMatcher.withDefaults().matcher("/actuator/**"))
              .authorizeHttpRequests(auth -> auth
                      .requestMatchers("/actuator/health/**")
                      .permitAll()
                      .anyRequest()
                      .hasRole("OPERATOR"));
      return http.build();
  }
  ```

- `httpBasic(Customizer.withDefaults())` is the Boot 2 `httpBasic()` in its
  surviving spelling; `csrf(csrf -> csrf.disable())` is the stateless API's
  decision, explained below. The `throws Exception` is optional from 7.0
  onward - `build()` lost its checked exception in that release, having
  declared one through 6.5 - but keeping it costs nothing and matches the
  `build()` call's history.
- The rules are ordered, and order is the whole review: the two `permitAll`
  rules come first, then `hasRole("MEMBER")` on the lending endpoints, then
  `anyRequest().authenticated()`. The [Title](../GLOSSARY.md) catalogue and the
  health probe are public, borrowing needs an authenticated Member, and
  *everything else* - including every other actuator endpoint - needs an
  authenticated principal. `/actuator/health/**` is the M09 probe path;
  exposing the rest of the management surface is a separate decision M09's
  never-list already made.
- `hasRole("MEMBER")` and the use case's `@PreAuthorize` answer two different
  questions, and the Lab needs both. The URL rule answers *may this principal
  call the lending API at all* - a principal without the role gets `403`
  before the use case runs. The expression answers *whose Loan is this* - it
  refuses any call whose `memberId` argument is not the principal's name, so a
  path that ever supplies a different id gets `403` from the method rather
  than a Loan in somebody else's name. Drop either one and a different caller
  gets through.
- `SessionCreationPolicy.STATELESS` says the API has no session to create.
  That is not a performance preference; it is the premise of the
  `csrf.disable()` line. A chain that disables CSRF while keeping a session is
  the defect this section exists to prevent.

A second chain is a second bean, and its order is explicit: `@Order(1)` on the
chain with a `securityMatcher`, `@Order(2)` on the chain without one. Spring
Security walks the chains in order and uses the first whose matcher accepts
the request; a chain with no matcher accepts everything, so it must come last.
Without the ordering the no-matcher chain wins and the management chain is
dead code - the ordering is the difference between "two chains" and "one chain
and a bean nobody reads". The management chain above keeps the M09 probe
public and gates every other actuator endpoint behind `OPERATOR`; once it
exists it owns `/actuator/**`, so the API chain's own health rule never fires -
harmless, and worth deleting for the reader who wants one rule per path.

### Method security: the caller is the argument

The URL rules say *which paths* need a [Member](../GLOSSARY.md). They cannot
say *which Member* may borrow which [Copy](../GLOSSARY.md), because that
answer is in the use case's argument list. Method security is the layer that
reads it, and it is switched on explicitly - Boot does not enable it for you:

```java
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfiguration {}
```

`@EnableMethodSecurity` in 7.1.1 defaults to `prePostEnabled = true`,
`securedEnabled = false`, `jsr250Enabled = false` and `mode = PROXY` - the
four defaults that matter, read off the annotation's `AnnotationDefault`
entries. The old `@EnableGlobalMethodSecurity` is still in the jar, still
deprecated, and its `prePostEnabled` still defaults to **false**; a Boot 2
configuration that
relied on the attribute-less form has method annotations that never ran, which
is the quietest upgrade defect in this Module. Use the new annotation and the
`@PreAuthorize` expression is live with no attribute at all.

The expression's job is to make the use case's argument and the authenticated
principal the same person:

```java
@Service
public class LoanService {

    private final LoanRepository loans;
    private final LoanProperties properties;
    private final Clock clock;

    LoanService(LoanRepository loans, LoanProperties properties, Clock clock) {
        this.loans = loans;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * The expression is the rule: the memberId the use case acts on must be
     * the authenticated principal's name.
     */
    @PreAuthorize("#memberId == authentication.name")
    @Transactional
    public Loan borrow(String memberId, Copy copy) {
        if (loans.countByMemberId(memberId) >= properties.limit()) {
            throw new LoanLimitExceededException(properties.limit());
        }
        if (loans.takeOnLoan(copy.id()) == 0) {
            throw new CopyNotAvailableException(copy.barcode());
        }
        LocalDate borrowedOn = LocalDate.now(clock);
        LocalDate dueOn = borrowedOn.plusDays(properties.period().toDays());
        Long loanId = loans.save(memberId, copy.id(), borrowedOn, dueOn);
        return new Loan(loanId, memberId, copy.barcode(), borrowedOn, dueOn);
    }
}
```

Two things have to be true for that line to mean anything, and the expression
is only one of them. The first is that parameter names survive compilation:
the expression's `#memberId` is a name, not a position, and this course's root
POM sets `maven.compiler.parameters=true` (Boot 4 does not), so the name is in
the class file. The second is that the argument *is* the authenticated name.
If the controller passes `request.memberId()`, the comparison is body value
against principal name: a Member who sends their own id passes, and a Member
who sends somebody else's id is refused with a `403` - the check itself is
real. What is not real is the contract. The API still asks the caller to name
the Member, so the borrow stays honest only because this one method remembers
the annotation; the next endpoint that takes a `memberId` and forgets it is a
cross-Member write. The fix is at the edge - stop accepting the id:

```java
@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loans;
    private final AvailabilityClient availabilityClient;
    private final CopyRepository copies;

    LoanController(LoanService loans, AvailabilityClient availabilityClient, CopyRepository copies) {
        this.loans = loans;
        this.availabilityClient = availabilityClient;
        this.copies = copies;
    }

    @PostMapping
    public ResponseEntity<Loan> borrow(@Valid @RequestBody LoanRequest request, Authentication authentication) {
        // The M04 availability call still runs before any transaction exists.
        Availability availability = availabilityClient.availability(request.copyId());
        if (!availability.available()) {
            throw new CopyNotAvailableException(request.copyId());
        }
        Copy copy = copies.findByBarcode(request.copyId())
                .orElseThrow(() -> new CopyNotFoundException(request.copyId()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(loans.borrow(authentication.getName(), copy));
    }
}
```

The request record loses its `memberId` field; the `Authentication` the
container injects is the only source of the Member's identity; and the
`@PreAuthorize` expression becomes a second lock on the same door rather than
the only one. That is the shape the Lab asks for: the id is not validated, it
is not supplied at all.

### CSRF: on by default, and what that means for an API

CSRF is a confused-deputy attack, and the sentence to keep is: *a browser
attaches cookies to requests it did not intend to make.* The attack works
because the browser, not the page, decides which cookies travel; the defence
is a token the attacker's page cannot read, required on every state-changing
request. In Security 5.7 the token lived in the session behind a
`LazyCsrfTokenRepository`; in 6.5 and 7.0 it is loaded through a
`DeferredCsrfToken` and rendered through an
`XorCsrfTokenRequestAttributeHandler`, which makes the token's response
encoding unpredictable per request and blunts BREACH-style compression
attacks. The unsafe-method set never moved: `GET`, `HEAD`, `TRACE` and
`OPTIONS` are exempt, and everything else - `POST`, `PUT`, `PATCH`, `DELETE` -
needs the token.

A browser application on 7.1.1 keeps the defaults and adds the SPA switch:

```java
@Bean
SecurityFilterChain browserApp(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .formLogin(Customizer.withDefaults())
            .csrf(csrf -> csrf.spa());
    return http.build();
}
```

`csrf.spa()` is new in 7.0 and does not turn CSRF off: it sets the repository
to `CookieCsrfTokenRepository.withHttpOnlyFalse()` and installs a
`SpaCsrfTokenRequestHandler` that accepts the plain header token a
single-page application reads from its cookie, falling back to the XOR
parameter encoding for a form post. That is the arrangement the old
`CookieCsrfTokenRepository` setters were assembled by hand for. The repository
itself narrowed in the same release: `setCookieHttpOnly`, `setCookieDomain`,
`setSecure` and `setCookieMaxAge` are gone, and the surviving surface is one
customizer plus the name and path setters:

```java
@Bean
CookieCsrfTokenRepository csrfRepository() {
    CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    repository.setCookieCustomizer(cookie -> cookie.sameSite("Strict"));
    return repository;
}
```

For the library's JSON API the decision is the opposite one, and it is a
decision, not a default: a stateless chain with no session and no cookie
carries no ambient credential for a browser to attach, so there is nothing for
CSRF to protect. That is why the API chain above disables it *and* sets
`STATELESS` in the same block; the two lines are one argument. The chain that
authenticates with a cookie - form login, an SPA session - keeps CSRF on, and
the test that proves it is a `POST` without a token answering `403` on a chain
that keeps CSRF on - our 4.1.1 run observed exactly that with an authenticated
principal on Boot's default chain.

The nuance a review should expect is the order of the two refusals, and it
runs the other way round from the obvious guess. A `POST` with no token
answers `403` whether or not the caller is authenticated, because 7.1.1's
`CsrfFilter` carries its own `AccessDeniedHandler` - an
`AccessDeniedHandlerImpl` by default - and answers the refusal itself, before
the request reaches authorization. Send a valid token instead and the
unauthenticated caller gets `401`: the request now passes the CSRF filter and
fails at authentication. Both were observed on a 4.1.1 boot, and the
authenticated `403` is the one the diagnostics assert.

### Passwords: the hash names its algorithm

A `PasswordEncoder` bean is the whole configuration, and the delegating
encoder is the one that survives an algorithm migration:

```java
@Bean
PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
}
```

`createDelegatingPasswordEncoder()` in 7.1.1 still defaults to `bcrypt` (we
read the factory's `ldc "bcrypt"`), still writes the algorithm id into the
stored value, and still reads it back on `matches`. A stored
`{bcrypt}$2a$10$...` can sit next to a `{pbkdf2}...` and an `{argon2}...` in
the same table; the prefix tells the encoder which one to use, and the
`PasswordEncoder` interface's `upgradeEncoding` is the hook a rehash-on-login
policy uses. Security 7.0 adds Password4j's encoders -
`Password4jPasswordEncoder` and the Argon2, Bcrypt, Pbkdf2, Scrypt and
BalloonHashing variants - for the deployment that wants Password4j's tuning
rather than Spring's own implementations. The delegating factory's contract
did not change.

Two `{id}` values matter to the Lab. `{bcrypt}` is what the factory produces
for a new password, and it is the value a production row should hold. `{noop}`
is the delegating encoder's plaintext escape hatch, and it is what Boot's
default principal uses when `spring.security.user.password` has no `{id}`
prefix: `UserDetailsServiceAutoConfiguration` prepends the constant
`{noop}` to the configured value. That is safe for exactly one thing - the
generated development principal Boot prints at boot - and it is the line a
review deletes before production. The 4.1.1 bytecode carries the same
`NOOP_PASSWORD_PREFIX` constant as 2.7.18, so this is not a Boot 4 change; it
is a Boot habit that becomes visible the moment you configure a password by
hand.

### Resource servers: a token is not a session

A resource server is the other authentication model, and it is one starter and
one configurer away:

```java
@Bean
SecurityFilterChain resourceServer(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/**")
                    .hasAuthority("SCOPE_library.read")
                    .anyRequest()
                    .authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable());
    return http.build();
}
```

The starter is `spring-boot-starter-security-oauth2-resource-server` on 4.1.1.
The old `spring-boot-starter-oauth2-resource-server` still resolves - we
fetched its POM - but its description says "deprecated in favor of
`spring-boot-starter-security-oauth2-resource-server`", so the new name is the
one a 4.1.1 POM should carry. The configurer is `Customizer`-only: 7.1.1's
`OAuth2ResourceServerConfigurer` has `jwt(Customizer)` and
`opaqueToken(Customizer)` and no no-arg `jwt()` or `opaqueToken()`; a Boot 2
`http.oauth2ResourceServer().jwt()` is a compile error twice over.

The chain's three lines are one argument again. A bearer token is not a
cookie: it is attached by the client's code, not by the browser's cookie jar,
so a cross-site page cannot make the request carry it. There is no session to
fix, hence `STATELESS`; there is no ambient credential, hence `csrf.disable()`.
The authority name is the token's claim by default (`scope` becomes
`SCOPE_...`), and a token whose claim is named something else needs the
converter:

```java
@Bean
JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
    authorities.setAuthoritiesClaimName("permissions");
    authorities.setAuthorityPrefix("SCOPE_");
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authorities);
    return converter;
}
```

Wire that converter into the `jwt(...)` customizer and
`hasAuthority("SCOPE_library.read")` starts reading the `permissions` claim
instead of `scope`. The lesson is the same one method security taught: the
rule is only as good as the value it reads.

### Where the pieces live in Boot 4

If the change names an auto-configuration class, the import is the Boot 4
one. `SecurityAutoConfiguration`, `UserDetailsServiceAutoConfiguration` and
the servlet chain's `ServletWebSecurityAutoConfiguration` (with its
`SecurityFilterChainConfiguration`) live in `spring-boot-security`, under
`org.springframework.boot.security.autoconfigure`; the 3.5.16 equivalents are
still in `spring-boot-autoconfigure` under
`org.springframework.boot.autoconfigure.security.servlet`. The starter's name
did not change, which is why the application boots while a hand-written
`@Import` of the old class fails - the same shape as M09's moved health
indicator.

The test side has its own starter. `@WithMockUser` lives in
`spring-security-test`, and Boot 4 packages it as
`spring-boot-starter-security-test`; `spring-boot-starter-test` alone leaves
the annotation off the classpath. That is a one-line POM change in the Lab,
and it is the difference between a security test that runs and a test that
does not compile.

## The Lab

The Lab is the library application you last touched in M09 - the borrow
commits atomically, the [Copy](../GLOSSARY.md) guard is one statement, the
[Loan](../GLOSSARY.md) rows live in the database, the probes answer and the
metric is published. None of it is protected: `POST /api/loans` still takes a
`memberId` from the request body, so any caller can borrow as any
[Member](../GLOSSARY.md), and the [Loan limit](../GLOSSARY.md) is counted
against whichever id the body supplied. The security upgrade is the Lab.

Your job on the Start module is the same three moves this Module taught, in
this order:

1. **Authenticate the Member.** Add `spring-boot-starter-security` (and
   `spring-boot-starter-security-test` in test scope), declare the chain as a
   `SecurityFilterChain` bean, keep the title catalogue and the M09 health
   probe public, require the `MEMBER` role on `/api/loans/**`, and require
   authentication for everything else. No `WebSecurityConfigurerAdapter`, no
   `authorizeRequests()`, no `and()`.
2. **Derive the Member from the principal.** The controller stops reading
   `memberId` from the request and passes `authentication.getName()` to the
   use case instead. The request record keeps only what the caller is allowed
   to choose - the [Copy](../GLOSSARY.md)'s barcode - and the
   [Loan](../GLOSSARY.md) row belongs to the authenticated Member.
3. **Apply the Loan limit to that Member.** Turn on `@EnableMethodSecurity`,
   put `@PreAuthorize("#memberId == authentication.name")` on the borrow use
   case, and make sure the count the limit reads is the authenticated
   Member's - not a global count, and not a count keyed by a request field.
   The M08 boundary and the M09 metric stay where they are; security is a
   layer above them, not a rewrite of them.

The rule generalises to the next feature: when the
[Reservation](../GLOSSARY.md) endpoints arrive, a hold belongs to the Member
who placed it, and the same `authentication.getName()` rule decides whose hold
the use case may create or cancel. The Lab's diagnostics do not test the
Reservation feature, but the shape they teach is the shape it will use.

The requirement is stated by Diagnostic tests in the `diagnostics` package -
the tests that fail on the Start module (which authenticates nobody and still
trusts the body) and pass on the Solution module, which is written from
scratch rather than patched. The command that states the requirement, the one
the [lab index](../labs/README.md) entry teaches, is:

```bash
./mvnw -f labs -pl lab-10-security7-start verify -Pdiagnostics
```

Read the failure messages the way this course reads every delta table row:
each names the behaviour that is missing - an unauthenticated borrow that
should answer `401`, a principal without the Member role that should answer
`403`, a body that names another [Member](../GLOSSARY.md) and must still
produce a [Loan](../GLOSSARY.md) for the authenticated one, a limit that
should count the caller's [Loan](../GLOSSARY.md)s. The same tests
pass against `lab-10-security7-solution` (and, in CI, against every Solution
module at once with `./mvnw -f labs verify -Pdiagnostics-solutions`); read that
module last, to see how small the secure story is once the chain is a bean and
the Member comes from the principal.

## Quiz

Five questions about the delta table above and the security register it
equips you to review. Answer first; the answers say why, not just what.

1. **About the chain rows: a Boot 2 codebase upgrades to 4.1.1. Its
   `WebSecurityConfigurerAdapter` subclass does not compile, and neither does
   the chain inside it. Name the two releases that produced the two errors,
   the class and the methods each release removed, and the smallest 7.1.1
   rewrite that preserves the old chain's meaning.**

   Two releases, two different kinds of break. Security 6.0 removed
   `WebSecurityConfigurerAdapter` - it was deprecated in 5.7 and gone by
   6.5.11 - so the subclass has no superclass and the first error is the
   `extends` clause. The replacement is a `@Bean SecurityFilterChain` that
   takes `HttpSecurity`; there is no `configure` override in the new model.
   Security 7.0 then removed the deprecated DSL: `authorizeRequests()`,
   `and()`, the no-arg `csrf()` / `formLogin()` / `httpBasic()` /
   `oauth2ResourceServer()`, and the `AntPathRequestMatcher` and
   `MvcRequestMatcher` classes. The smallest rewrite is the one this Module
   writes: `http.authorizeHttpRequests(auth -> auth.requestMatchers(...)
   .permitAll().anyRequest().authenticated()).httpBasic(Customizer.withDefaults())`
   and `return http.build();`. The meaning is preserved because the rules are
   the same ordered list; what changed is that the registry's methods return
   the registry and the configurers take a lambda, so the `and()` that used to
   climb back to the `HttpSecurity` has nothing left to climb.

2. **About the CSRF row and the section that separates the three releases: a
   colleague's upgrade notes say "Boot 4 changed the CSRF defaults, so our
   `POST` endpoints now need a token." State what Security 6.5 already has,
   what arrived in Security 7.0, and what Boot 4 changed - then say whether the
   colleague's `POST` endpoints needed the token before the upgrade.**

   The note has the right symptom and the wrong release. Security 6.5 already
   has the modern defaults: `CsrfConfigurer`'s repository is
   `HttpSessionCsrfTokenRepository`, `CsrfFilter`'s request handler is
   `XorCsrfTokenRequestAttributeHandler`, the token is loaded as a
   `DeferredCsrfToken`, and the unsafe-method set is everything except `GET`,
   `HEAD`, `TRACE` and `OPTIONS`. Security 7.0 keeps every one of those and
   adds `csrf.spa()` plus the narrowed `CookieCsrfTokenRepository` surface
   (`setCookieCustomizer` in place of the removed `setCookieHttpOnly`,
   `setCookieDomain`, `setSecure`, `setCookieMaxAge`). Boot 4 changed none of
   it: 4.1.1's default `SecurityFilterChain` still calls
   `authorizeHttpRequests(...)`, `formLogin(...)` and `httpBasic(...)` without
   touching CSRF, and the filter's defaults match 6.5's. So the colleague's
   `POST` endpoints needed the token before the upgrade too - if they were
   session-authenticated, CSRF was already on by default, and the upgrade did
   not turn it on. The real question is the one the section asks: is the chain
   session- and cookie-based? If yes, keep CSRF on and send the token; if it
   is stateless with a bearer token, `csrf.disable()` is the deliberate,
   documented exception, not a response to a Boot 4 change.

3. **About the method-security section: a reviewer says
   `@PreAuthorize("#memberId == authentication.name")` is enough to stop one
   Member borrowing as another, so the controller can keep passing
   `request.memberId()`. Say what the expression compares and what happens to a
   Member who sends somebody else's id - then say why the reviewer's conclusion
   is still the wrong one, and what the controller has to change. Name the two
   annotation defaults that decide whether the check runs at all.**

   The expression compares the use case's first argument - `#memberId`, read
   from the compiled parameter name - with the authenticated principal's
   `name`. With `request.memberId()` still in the controller, the left side is
   whatever the caller put in the body and the right side is the caller's
   authenticated name: a Member who sends their own id gets a true comparison
   and a [Loan](../GLOSSARY.md) in their own name, and a Member who sends
   somebody else's id gets a false comparison and a `403`. So the cross-Member
   write is refused today - the reviewer's premise is right about that one
   call. The conclusion is wrong because it makes the API's safety a property
   of one annotation on one method: the request still *offers* the caller a
   `memberId` to choose, and the next endpoint that reads the field and
   forgets the expression - a list, a cancel, a second use case - is open. The
   controller stops accepting the id at all: drop `memberId` from the request
   record and pass `authentication.getName()` to the use case. The expression
   then compares the principal with itself, and the value the caller could
   choose is gone rather than merely checked. The two defaults:
   `@EnableMethodSecurity`'s `prePostEnabled = true` (so `@PreAuthorize` runs
   with no attribute) and `@EnableGlobalMethodSecurity`'s
   `prePostEnabled = false` (so the old annotation without the attribute runs
   nothing). That difference is why the annotation choice is not cosmetic.

4. **About the password section: a stored row begins `{noop}secret`, and a
   colleague says "the encoder will hash it on the next login." Explain what
   the delegating encoder does with that prefix, what Boot writes when
   `spring.security.user.password=secret` has no prefix, and the two-line
   change that makes the row production-shaped.**

   `{noop}` is the delegating encoder's plaintext encoder: it compares the
   submitted value with the stored value directly and never rehashes it. The
   prefix is the encoder's instruction, not a comment; `{bcrypt}` selects
   BCrypt, `{pbkdf2}` selects PBKDF2, and `{noop}` selects "store it in the
   clear". Boot's `UserDetailsServiceAutoConfiguration` prepends `{noop}` to a
   configured `spring.security.user.password` that has no `{id}` prefix - the
   `NOOP_PASSWORD_PREFIX` constant is in the 2.7.18 and 4.1.1 bytecode alike -
   so the generated development principal works without an encoder bean, and
   the same convenience becomes a production defect the moment the property is
   set by hand. The production shape is two lines: declare the
   `PasswordEncoder` bean (the delegating factory above) and store the value
   the encoder produced - `{bcrypt}$2a$...` - by calling
   `passwordEncoder.encode("secret")` at provisioning time, not by writing the
   plaintext into the property. The `{noop}` row is then a row a review
   refuses to call secure, which is exactly what the change should show.

5. **About the resource-server section: a colleague wants to reuse the
   library's browser chain for an API client, so they keep the session,
   enable `httpBasic`, and leave CSRF on. The client sends a bearer token.
   Name the two things that go wrong, the starter and configurer spelling on
   4.1.1, and why `csrf.disable()` is correct for the token chain but wrong
   for the browser chain.**

   The first thing that goes wrong is authentication: `httpBasic` reads an
   `Authorization: Basic` credential, not a bearer token; a JWT sent to a
   Basic entry point is an unreadable header, and the request is
   unauthenticated. The second is the session: a bearer-token client that is
   given a session gets a `JSESSIONID` cookie it did not ask for, and the
   chain that was supposed to be stateless starts carrying an ambient
   credential - which is the condition CSRF exists for, now with CSRF on and
   no token in the client's requests. On 4.1.1 the resource server is
   `spring-boot-starter-security-oauth2-resource-server` (the old
   `spring-boot-starter-oauth2-resource-server` still resolves but is
   deprecated) and the configurer is
   `oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))`,
   with no no-arg `jwt()` left. `csrf.disable()` is correct for the token
   chain because the token is attached by the client's code, not by the
   browser's cookie jar: a cross-site page cannot make the request carry it,
   so there is no confused deputy to defend against. It is wrong for the
   browser chain because the browser does attach the session cookie, and the
   token is the only thing that distinguishes the Member's own request from a
   forged one. The two lines - `STATELESS` and `csrf.disable()` - are one
   argument, and the argument is "no ambient credential".

## Checklist

What a reviewer applies to a solution of this Lab to judge it
production-shaped rather than merely working. Every item is observable -
checkable by reading the change or running a command - and none of them
repeats a gate the build already enforces.

- **The chain is a `SecurityFilterChain` bean.** The change contains no
  `WebSecurityConfigurerAdapter`, no `authorizeRequests()`, no `antMatchers()`
  or `mvcMatchers()`, and no `and()`. Those are the four searches that answer
  in seconds, and any hit is a Boot 2 fragment that survived the upgrade by
  not being compiled.
- **Every configurer call uses the `Customizer` form.** `csrf(...)`,
  `formLogin(...)`, `httpBasic(...)` and `oauth2ResourceServer(...)` each take
  a lambda or `Customizer.withDefaults()`; no no-arg overload is called,
  because 7.1.1 has none. `authorizeHttpRequests(...)` is the only
  authorization entry point.
- **The only matcher class named is `PathPatternRequestMatcher`.** The diff
  contains no `AntPathRequestMatcher` and no `MvcRequestMatcher` - they are
  not in the 7.1.1 jar - and a `securityMatcher` that needs an explicit
  matcher builds it with `PathPatternRequestMatcher.withDefaults()`.
- **The public surface is exactly the surface that should be public.** The
  [Title](../GLOSSARY.md) catalogue's read endpoints and `/actuator/health/**`
  are permitted; `/api/loans/**` requires the `MEMBER` role; every other
  actuator endpoint is not public, and `anyRequest().authenticated()` closes
  the chain. The M09 never-list still holds: the management surface is not
  re-exposed by a convenience `permitAll`.
- **The Member is the authenticated principal.** The borrow request carries
  the [Copy](../GLOSSARY.md)'s barcode, not a `memberId`; the controller
  passes `authentication.getName()`; and the use case's
  `@PreAuthorize("#memberId == authentication.name")` is a second lock, not
  the only one. A request with no authentication answers `401`; a principal
  without the Member role answers `403`; a body that still carries another
  Member's id is ignored, and the [Loan](../GLOSSARY.md) lands on the
  authenticated Member.
- **Method security is switched on and the old annotation is not used.**
  `@EnableMethodSecurity` is on a configuration class; there is no
  `@EnableGlobalMethodSecurity` in the change (it is present in 7.1.1 but
  deprecated, and its `prePostEnabled` default is false). The expression's
  parameter names compile because this course's root POM sets
  `maven.compiler.parameters=true`.
- **The [Loan limit](../GLOSSARY.md) counts the authenticated Member's
  [Active loans](../GLOSSARY.md).** The
  count is keyed by the principal's name, not by a request field and not by a
  global total; the M08 boundary still wraps the count and the write, and the
  M09 metric is still published where the [Loan](../GLOSSARY.md) is issued.
- **CSRF is on for every session- or cookie-authenticated chain, and off only
  where the chain is stateless and token-authenticated.** The exception is
  written down in the change, next to the `STATELESS` line that justifies it.
  If a `CookieCsrfTokenRepository` is configured, it uses
  `setCookieCustomizer` - the four individual cookie setters are gone - and
  `csrf.spa()` is used for a single-page application rather than a hand-rolled
  token dance.
- **Passwords are stored with an algorithm prefix.** A `PasswordEncoder` bean
  is declared; no plaintext password sits in `application.properties` or in a
  migration; `{noop}` appears nowhere outside the generated development
  principal; and a stored `{bcrypt}` value is what the encoder produced, not a
  value copied from a Boot 2 `BCryptPasswordEncoder` row without its prefix.
- **The resource-server spelling is 4.1.1's.** The POM names
  `spring-boot-starter-security-oauth2-resource-server`, not the deprecated
  `spring-boot-starter-oauth2-resource-server`; the chain calls
  `oauth2ResourceServer(oauth2 -> oauth2.jwt(...))`, not the removed no-arg
  form; and the token's authorities are mapped deliberately when the claim is
  not `scope`.
- **The test dependency is the security one.** `spring-boot-starter-security-test`
  is in test scope, so `@WithMockUser` is on the classpath; the tests assert
  the behaviour - `401` unauthenticated, `403` for a principal without the
  Member role, `201` for the owner's borrow, and, on any session chain the
  change keeps, `403` for a `POST` without a CSRF token - not the arrangement
  of the configuration classes.
- **Boot 4 imports are the Boot 4 packages.** Any reference to Boot's security
  auto-configuration uses `org.springframework.boot.security.autoconfigure.*`,
  not `org.springframework.boot.autoconfigure.security.servlet.*`; the
  starter name `spring-boot-starter-security` is unchanged, which is why the
  application boots while a hand-written old import does not.
- **The vocabulary is the glossary's in the names, not just in the prose.**
  The principal is a [Member](../GLOSSARY.md), the thing borrowed is a
  [Copy](../GLOSSARY.md), the row is a [Loan](../GLOSSARY.md), and the limit
  is the [Loan limit](../GLOSSARY.md) - not "user", "book" or "checkout" - so
  the authorization rule, the error message and the test names all speak the
  same language as the domain.
- **The diagnostics guarantee is proved by a failing-then-passing run.** Run
  it both ways and report what ran:
  `./mvnw -f labs -pl lab-10-security7-start verify -Pdiagnostics` (red - the
  Start module authenticates nobody), then the same against the `-solution`
  module (green - authentication, ownership and the per-Member limit hold);
  the asymmetry is the requirement, stated exactly as M08's and M09's ran.
