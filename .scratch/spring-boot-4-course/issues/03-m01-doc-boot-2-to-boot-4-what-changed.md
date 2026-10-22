# 03: M01 doc: Boot 2 to Boot 4: what changed

**What to build:** The Module doc for M01: what the reader knew in Boot 2, what changed in Boot 3 and Boot 4, and how it works now in the version this course builds against. It covers the Jakarta namespace move, the Jackson 2 to Jackson 3 move and its new package root, the modularised starters and the one that was only deprecated, where the webmvc test slice moved, and the Spring Framework 6 and 7 changes that reach everyday code, uses the glossary vocabulary, and points at the Lab that follows it.

**Blocked by:** 01, 02

**Status:** resolved

- [x] the doc states the Boot 2 to Boot 3 to Boot 4 delta as a table, covering Jakarta, Jackson 3, the modularised starters and the relocated test slice
- [x] every claim in the delta table is checked against the version in use, not remembered
- [x] the terms it introduces are the glossary's terms, and are linked
- [x] it names the Lab, and the command that runs the Lab's diagnostics
- [x] every snippet in it compiles against the version in use
- [x] it states what the reader is assumed to know and what they are not

## Answer

Built in commit `e3c770a`, corrected in `4b6da99`.

- **The Module doc** `modules/01-boot2-to-boot4.md` follows the authoring
  guide's skeleton: what the reader knew, the Boot 2 to Boot 3 to Boot 4 delta
  table, how it works now, the Lab, the quiz, the checklist.
- **Every claim in the delta table was checked against the versions in use**,
  not remembered:
  - Jackson 2.13.5 (Boot 2.7) -> 2.14.1 (3.0) -> 2.21.4 (3.5) -> 3.1.5 (4.1.1).
  - The deprecated web starter still resolves in Boot 4 - deprecated, not
    removed - and `spring-boot-starter-webmvc` does not exist before Boot 4.
  - Boot 4 renamed `spring-boot-starter-json` to
    `spring-boot-starter-jackson`. Corrected in `4b6da99` after checking the
    published POMs.
  - The webmvc test slice left `spring-boot-starter-test` and moved its
    package to `org.springframework.boot.webmvc.test.autoconfigure`.
  - `@MockBean` is deprecated in Boot 3.5 and gone in Boot 4; `@MockitoBean`
    lives in spring-test, not spring-boot.
  - `org.springframework.resilience` and API versioning are Framework 7
    additions - absent from Framework 6.2.19.
  - Boot 2.7 manages both `javax.validation` and `jakarta.validation`; Boot 3
    dropped `javax`.
- **The Jackson 3 annotations did not move** - `jackson-annotations` is still
  `com.fasterxml.jackson.core:jackson-annotations` at 2.21 inside a Jackson 3
  application. The doc says so, because it is the detail everyone guesses
  wrong.
- **Every snippet was checked against the solution module's real classpath**,
  and all five resolved - including the two that come from different Jackson
  generations.
