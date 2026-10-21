# 58: M12 doc: Native images and packaging

**What to build:** The Module doc for M12: what the reader knew in Boot 2, what changed in Boot 3 and Boot 4, and how it works now in the version this course builds against. It covers GraalVM native images in Boot 4, ahead-of-time processing, what breaks at build time rather than at run time, container images and layer jars, and what a native build costs, uses the glossary vocabulary, and points at the Lab that follows it.

**Blocked by:** 57

**Status:** ready-for-agent

- [ ] the doc states the Boot 2 to Boot 3 to Boot 4 delta as a table, covering ahead-of-time processing, native images and packaging
- [ ] every claim in the delta table is checked against the version in use, not remembered
- [ ] the terms it introduces are the glossary's terms, and are linked
- [ ] it names the Lab, and the command that runs the Lab's diagnostics
- [ ] every snippet in it compiles against the version in use
- [ ] it states what the reader is assumed to know and what they are not
