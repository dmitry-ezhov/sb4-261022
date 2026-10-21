# 60: M12 lab solution: Native images and packaging

**What to build:** The Solution module for M12: the reference implementation of packaging the application, producing a native image, and making the Lab's diagnostics pass in the packaged form rather than only in the exploded one, written from scratch rather than patched from the Start module, satisfying every diagnostic of the Lab.

**Blocked by:** 59

**Status:** ready-for-agent

- [ ] the Solution module exists and is listed in the labs aggregator
- [ ] it is written from scratch, not patched from the Start module
- [ ] every diagnostic of the Start module passes against it
- [ ] it meets the coverage floor
- [ ] its own tests assert the Lab's behaviour, not the implementation's arrangement
- [ ] CI asserts its diagnostics through the single diagnostics command
