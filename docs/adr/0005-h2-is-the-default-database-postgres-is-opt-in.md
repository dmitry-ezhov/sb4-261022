# H2 is the default database, Postgres is opt-in

The data track runs on H2 by default. The persistence, concurrency and
transaction labs add PostgreSQL through Testcontainers, and Docker is documented
as an optional prerequisite rather than a required one.

We did this because the course is self-paced and self-pacing means people work
on it when Docker is not running, when their laptop is on a train. H2 keeps
everybody moving through the labs. The labs where the database choice actually
changes the answer - dialect differences, real locking, transaction isolation -
are exactly the ones that offer Testcontainers.
