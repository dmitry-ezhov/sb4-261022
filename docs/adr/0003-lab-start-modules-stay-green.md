# Lab start modules stay green

A start module compiles, is formatted, passes every static analysis rule and
passes its own tests. The defects it contains are behavioural only, and they are
described by diagnostic tests that are excluded from the default build and run
only under the `diagnostics` profile.

We did this because a start module that is red from the start teaches the wrong
thing: the learner cannot tell "the build is broken" from "the lab is not done
yet". Keeping the default build green means every red build is a red build the
learner caused, and `mvn -f labs verify -Pdiagnostics` is the single command
that shows what is still missing. The coverage floor is the one gate start
modules opt out of, because they ship fewer tests than their solution.
