# Quality gates fail the build, with a lenient escape profile

Spotless, PMD, SpotBugs, Checkstyle and the JaCoCo coverage floor all fail
`mvn verify` when they are not satisfied. A `quality-lenient` profile turns the
same configuration into warnings for the moments when a learner is mid-task and
only wants to compile.

We did this because the alternative - a build that never fails - teaches nothing,
while a build that always fails with no escape hatch makes the first hour of
every lab a fight with the formatter. The escape hatch is opt-in and named, so a
build that skipped the gates is visible in the command line rather than hidden in
a POM.
