# 13: M03 doc: Typed configuration and validation

**What to build:** The Module doc for M03: what the reader knew in Boot 2, what changed in Boot 3 and Boot 4, and how it works now in the version this course builds against. It covers typed configuration properties, records versus classes, validation at startup, the property naming rules, and the configuration properties that Boot 4 changed, uses the glossary vocabulary, and points at the Lab that follows it.

**Blocked by:** 12

**Status:** resolved

- [ ] the doc states the Boot 2 to Boot 3 to Boot 4 delta as a table, covering typed configuration binding and startup validation
- [ ] every claim in the delta table is checked against the version in use, not remembered
- [ ] the terms it introduces are the glossary's terms, and are linked
- [ ] it names the Lab, and the command that runs the Lab's diagnostics
- [ ] every snippet in it compiles against the version in use
- [ ] it states what the reader is assumed to know and what they are not

## Answer

Built in commit `HEAD~0` (the commit that added this doc).

- **The Module doc** `modules/03-typed-configuration.md` follows the
  authoring guide's skeleton and covers the three ways to get a value, the
  record-based configuration class, the naming rules (relaxed binding,
  Boot's `Duration` shorthand, lists and maps), startup validation with
  `@Validated` and Jakarta constraints, and `@DefaultValue`.
- **Every claim in the delta table was checked against the versions in use**:
  - constructor binding is the default from Boot 3; Boot 2.7's own
    documentation says the `@ConstructorBinding` annotation "is used to
    indicate that constructor binding should be used", with records as the
    exception;
  - the constraint annotations moved namespaces: Boot 2.7's documentation
    says "JSR-303 `javax.validation`", Boot 4's says "`jakarta.validation`";
  - `spring-boot-starter-validation` exists in 2.7.18, 3.5.16 and 4.1.1;
  - `@ConfigurationPropertiesSource` is new in Boot 4 - absent from the
    3.5.16 source tree.
- **Two beliefs were corrected by checking**: `@DefaultValue` is not new -
  it is in Boot 2.7.18, 3.0.0 and 4.1.1 - so it gets no row in the delta
  table; and Boot 4's `ConfigurationProperties` carries no JSpecify
  annotations, so the doc makes no null-safety claim about the configuration
  API.
- **Every snippet resolves against the real classpath** of a lab module:
  `@ConfigurationProperties`, `@DefaultValue`, `@ConfigurationPropertiesSource`,
  `@ConfigurationPropertiesScan`, `@Validated`, `@NotNull`, `@Min` and Boot's
  `DurationStyle` (the `14d` shorthand).
- **The Lab** is described with its three Diagnostics: the loan period comes
  from the configuration, an invalid loan limit stops the application at
  startup, and the loan limit is bound and validated. The Lab adds the Loan
  feature to the application, so the course's vocabulary is complete.
