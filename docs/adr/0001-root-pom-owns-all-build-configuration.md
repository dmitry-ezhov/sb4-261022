# The root POM owns all build configuration

All dependency versions, plugin versions and quality gates live in the root
`pom.xml`, which is both the parent and the aggregator of the repository.
`labs/pom.xml` exists only to list the lab modules; it configures nothing of
its own.

We did this because the sibling course `sb4-robust-and-secure` keeps its build
configuration in `labs/pom.xml`, which means every artifact that needs to know
about the build - the parent, the aggregator, CI, future non-lab modules - has
to reach into a directory named after the labs. A single root parent keeps one
source of truth, and the cost is one extra file that holds nothing but a list of
modules.
