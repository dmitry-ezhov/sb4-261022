# 40: M08 lab solution: Transactions and consistency

**What to build:** The Solution module for M08: the reference implementation of making the borrow operation transactional so two concurrent borrows cannot take the same Copy, proved by a test that fails without the lock, written from scratch rather than patched from the Start module, satisfying every diagnostic of the Lab.

**Blocked by:** 39

**Status:** ready-for-agent

- [ ] the Solution module exists and is listed in the labs aggregator
- [ ] it is written from scratch, not patched from the Start module
- [ ] every diagnostic of the Start module passes against it
- [ ] it meets the coverage floor
- [ ] its own tests assert the Lab's behaviour, not the implementation's arrangement
- [ ] CI asserts its diagnostics through the single diagnostics command
