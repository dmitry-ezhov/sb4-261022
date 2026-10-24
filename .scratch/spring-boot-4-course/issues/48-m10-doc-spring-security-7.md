# 48: M10 doc: Spring Security 7

**What to build:** The Module doc for M10: what the reader knew in Boot 2, what changed in Boot 3 and Boot 4, and how it works now in the version this course builds against. It covers the filter chain in its lambda form, method security, resource servers, password encoding, the CSRF defaults Boot 4 changed, and what moved since Security 5, uses the glossary vocabulary, and points at the Lab that follows it.

**Blocked by:** 47

**Status:** resolved

- [x] the doc states the Boot 2 to Boot 3 to Boot 4 delta as a table, covering the filter chain, method security and the CSRF defaults
- [x] every claim in the delta table is checked against the version in use, not remembered
- [x] the terms it introduces are the glossary's terms, and are linked
- [x] it names the Lab, and the command that runs the Lab's diagnostics
- [x] every snippet in it compiles against the version in use
- [x] it states what the reader is assumed to know and what they are not

## Answer

The Module doc is `modules/10-security7.md`. Its delta table has the Boot 2 ->
Boot 3 -> Boot 4 columns against the three claims the ticket names - where the
chain is declared, how it is written, the method-security switch and the CSRF
defaults - and every row says which release moved it, including the ones that
did not move when the folklore says they did (Boot 4 kept the CSRF defaults;
6.5 -> 7.0 and 7.1.1 are two different breaks).

Checked, not remembered: the claims were read off the resolved artifacts -
Boot 4.1.1, Framework 7.0.9, Security 7.1.1, Java 25 - by `javap` on the
`HttpSecurity`/`AbstractSecurityBuilder` signatures (`build()` has no checked
exception since 7.0), by jar listings (`AntPathRequestMatcher` and
`MvcRequestMatcher` absent, `SpaCsrfTokenRequestHandler` present), by reading
the delegating encoder's `ldc "bcrypt"` and the Password4j class names out of
the crypto jar, and by booting 4.1.1 to watch the CSRF `403` arrive before the
authentication `401`. `labs/README.md` carries the verified-versions row.

Every "how it works now" snippet compiles against those exact jars (checked by
`javac` against the Solution module's 90-jar classpath), and the snippet the
reader is asked to trust - the API chain and the management chain - is the one
`labs/lab-10-security7-solution` ships, not a near miss: no `throws Exception`
(PMD's `SignatureDeclareThrowsException` is active in `config/pmd/ruleset.xml`,
so the declaration would cost a suppression), and the API chain carries no
actuator rule because the management chain owns `/actuator/**`.

The prose uses `GLOSSARY.md`'s terms with links on first use (Member, Title,
Copy, Loan, Reservation, Loan limit) and no synonyms. The Lab is named in `##
The Lab` with the command that states its requirement,
`./mvnw -f labs -pl lab-10-security7-start verify -Pdiagnostics`, and the
module opens by stating what the reader is assumed to know and what they are
not.

