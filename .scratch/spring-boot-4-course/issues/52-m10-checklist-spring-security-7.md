# 52: M10 checklist: Spring Security 7

**What to build:** The checklist for M10: what a reviewer applies to a solution of this Lab to judge it production-shaped rather than merely working.

**Blocked by:** 51

**Status:** resolved

- [x] the checklist names observable properties, not implementation steps
- [x] every item is verifiable by reading the change or running a command
- [x] it covers access is denied unless explicitly granted
- [x] it covers no secret is hard-coded or logged
- [x] it covers authorization is enforced at the boundary and in the domain
- [x] it covers the unauthenticated and the forbidden case are both tested
- [x] no item restates a Quality gate that the build already enforces

## Answer

The checklist was drafted with the doc (ticket 48); this ticket graded it
against its criteria and rebuilt the half that failed. Every rebuilt claim
was checked against the resolved lab-10 solution and the 7.1.1 jars, the way
ticket 47 graded its checklist.

- **The two meta-criteria met before the rebuild, and where the first
  failed.** The draft's first three items - no `WebSecurityConfigurerAdapter`
  or `authorizeRequests()` or `and()`, the `Customizer` form only, the one
  matcher class - name *deleted* names, not observable properties: all of
  them are gone from the 7.1.1 jar (`javap` on
  `spring-security-config-7.1.1.jar` shows `HttpSecurity` carrying only
  `Customizer` overloads), so compilation against it already refuses every
  state those diffs searched for - the build's first verdict restated as
  review steps. They became checks the compiler cannot do: the default-deny
  item (grants, closed walks, the load-bearing `@Order`), the method-security
  item (the one name a compiling change can still carry), and the
  auto-configuration item, whose 4.1.1 claim the read of the jars corrected
  along the way: `org.springframework.boot.autoconfigure.security.servlet.*`
  is absent from 4.1.1's `spring-boot-autoconfigure` entirely, so a Java
  import fails compilation but a class name inside an `application.yml`
  compiles against nothing - `spring.autoconfigure.exclude` and every
  printed FQCN in the resources is the one search the diff needs, and the
  item now says so.
- **Access is denied unless explicitly granted - half-met, rebuilt as the
  first item.** The draft named the grants and quoted
  `anyRequest().authenticated()`, but never asked whether the unnamed paths
  are closed *as observed*. The item now reads both rule lists' last lines
  and runs the three refusals the diagnostics already prove: health answers
  a stranger `200`, the same stranger's read of metrics is refused, and an
  unauthenticated reservation call is refused `401` - the
  [Reservation](../../../GLOSSARY.md) surface is answered by the closing
  rule, not by being named. The `@Order(1)`/`@Order(2)` split moved into the
  same item, because it is the walk's property: without the order the
  no-matcher API chain answers the management surface too.
- **No secret is hard-coded or logged - the logged half the draft left
  out.** The draft's passwords item covered the hard-coded half (encoder
  produced, `{bcrypt}` prefix, `{noop}` nowhere, no plaintext in properties
  or migrations) and never said the word *log*. The rebuilt item carries
  both halves under the ticket's own title, and the log half is grounded in
  the 4.1.1 bytecode: `UserDetailsServiceAutoConfiguration` in
  `spring-boot-security-4.1.1.jar` prints the generated principal's password
  on one warn line - `Using generated security password: %s` - and an
  application whose principal store is its own `UserDetailsService` bean is
  the change that stops the line from existing at all; the check is
  grepping one boot log for it.
- **Authorization at the boundary and in the domain - met, made explicit.**
  The retitled Member item carries both locks with their division of
  labour, the Lab's own words: the chain's `hasRole("MEMBER")` answers the
  role-less principal before the use case runs, and
  `@PreAuthorize("#memberId == authentication.name")` is the second lock
  for a path that ever supplies a different id - the expression reads
  arguments, not authorities, so the rule-less `403` can only have come
  from the chain. Verified by the diagnostics' messages, which name the
  row's owner and the limit's count against the principal.
- **The unauthenticated and the forbidden case are both tested - met in
  substance, tightened and given its real-chain ground.** The rebuilt item
  names what makes the pair the product's: the slice imports the change's
  own `SecurityConfiguration`, so `401` (authentication missing) and `403`
  (a qualifying identity that does not qualify - `@WithMockUser`'s
  `OPERATOR` principal in the module's own slice tests, real `operator`
  Basic credentials in the diagnostics) are two different refusals answered
  before the
  controller runs, with the `201` owner's borrow between them and the
  conditional CSRF refusal kept. The security-test starter's presence rides
  the same sentence: `@WithMockUser` is off the classpath without it.
- **Two claims corrected against the jars and the reference.** The draft's
  cookie-repository claim ("the four individual cookie setters are gone")
  was imprecise in both direction and spelling: 6.5.11 carries seven
  individual setters, 7.1.1 four fewer - `setCookieHttpOnly`,
  `setCookieDomain`, `setSecure` and `setCookieMaxAge` are gone,
  `setCookieName` and `setCookiePath` survive, and the item now says which
  four and points the attributes at `setCookieCustomizer`, matching the
  doc's CSRF section. And the resource-server item gained the conditional
  its placement needs - the Lab's solution carries no resource server (the
  POM check), so the item binds the 4.1.1 spellings only *where the change
  adds one*.
- **No item restates a gate.** The gates speak in formatting, defects and
  coverage; the checklist speaks in grants, refusals, secrets and stored
  credentials - and the one repetition the draft had (compilation's
  verdict), the rebuild removed instead of kept.
- **Confirmed with a run:** the checklist's own last item executed both
  ways - `./mvnw -f labs -pl lab-10-security7-start -am verify
  -Pdiagnostics` (red: 5 tests, 5 failures - the metrics read answered a
  stranger `200`, the borrows answered `201`, each message naming the
  missing grant or refusal), then the same against
  `lab-10-security7-solution` (green: 5 tests, 0 failures, every gate and
  the module's own tests passing). Red on the Start module, green on the
  Solution module - the asymmetry the item describes.

## Comments

- **2026-10-24 · agent** — Graded the ticket-48 checklist against the
  acceptance criteria; the deleted-names items were restated compilation
  and the criteria's logged-secrets and default-deny halves were open or
  half-met; each became or reshaped an item, with every rebuilt claim
  checked off the resolved lab-10 code and the 6.5.11/7.1.1 jars. Doc edit
  is the resolution; no code changed.
- **2026-10-24 · agent** — Review fix: the secrets item now carries the
  lab-fixtures clause a verbatim reading would trip over (the resolved
  solution's principal constants are the lab's fixtures; the item judges
  the stored shape and the named store swap, not the lab code), and the
  both-refusals item names the 403's two homes - `@WithMockUser`'s
  `OPERATOR` principal in the slice tests, real `operator` Basic
  credentials in the diagnostics - so the Answer's attribution cannot be
  read as the diagnostics carrying a mock.
