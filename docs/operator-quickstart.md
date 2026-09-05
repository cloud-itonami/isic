# Operator quickstart — `cloud-itonami/isic`

Every command below was executed on 2026-09-06 and is recorded with the
output it actually produced. The source under test is `f2572c5` unchanged —
the commit adding this file touches only documentation and
`tools/check-docs.cljs`. If a command here does not run for you, that is a
defect in this file, not in your setup — see *Keeping this file honest* at
the end.

## What you are starting

An **R0 scaffold**. `isic.coordinator` routes a state map by its mode and
answers the `NOT-FOUND-R0` sentinel, because the UN ISIC Rev.4 hierarchy has
not been ingested into `data/` yet. There is nothing to look a code up in.

That matters for what "green" means here: a clean run tells you **the
scaffold's routing is measured**. It does not tell you this cell resolves ISIC
codes, and no command in this file will make it do so.

## Prerequisites

| tool | used by | check |
|---|---|---|
| `nbb` | the portable suite, `tools/*.cljs` | `nbb --version` |
| `clojure` | `:test` and `:lint` aliases, the mutation harness | `clojure --version` |

`nbb` alone is enough for step 1, which is the step that answers "does this
cell work". The JVM is only needed for the lint and mutation steps, and this
workspace puts it below nbb on purpose (ADR-2607173000).

## 1. Run the portable suite (no JVM, no build step)

```sh
nbb --classpath src:test test/run_portable.cljs
```

```
Testing isic.coordinator-test

Ran 9 tests containing 13 assertions.
0 failures, 0 errors.
```

Exit status `0`. This is the whole point of the namespace being `.cljc`:
`src/isic/coordinator.cljc` contains **no reader conditionals**, so "it is
portable" is only true if something has actually loaded it under
ClojureScript. This runner is that something.

## 2. Run it from somewhere that is not this repo

```sh
cd /tmp && nbb --classpath <repo>/src:<repo>/test <repo>/test/run_portable.cljs
```

Same `Ran 9 tests containing 13 assertions. 0 failures, 0 errors.`, exit `0`.

This is a separate check, not a repetition. The cell touches no file at
runtime, so it must not care about the process's working directory — and a
suite run only from the repo root cannot tell you whether it does.

## 3. Run the JVM suite

```sh
clojure -M:test
```

```
Running tests in #{"test"}

Testing isic.coordinator-test

Ran 9 tests containing 13 assertions.
0 failures, 0 errors.
```

Exit status `0`. Same nine tests, same thirteen assertions, other runtime.
For **this** namespace the two runtimes see identical bytes, so agreement is
expected rather than informative; it stops being free the moment anyone adds
a reader conditional here.

## 4. Lint

```sh
clojure -M:lint
```

```
linting took NNNms, errors: 0, warnings: 0
```

Exit status `0`; the alias fails on `--fail-level error`. The duration
varies with machine load and is not part of the expected output — what must
hold is `errors: 0`.

Note the alias lints `src` and `test` only. `tools/` is nbb-only and is not
in its scope.

## 5. Prove the suite can fail

A suite that has never gone red is a suite nobody has measured, so the repo
carries its own mutation table. Check it before spending minutes on it:

```sh
nbb tools/check-mutations.cljs
```

```
SCANNED	5 mutations
all find strings occur exactly once
```

Then run the mutations. Each one is applied to a source file, `clojure -M:test`
is run, and the file is restored:

```sh
nbb tools/mutate.cljs           # the whole table
nbb tools/mutate.cljs :noop-does-not-stamp-the-state   # one, by id
```

**Expect one survivor.** `:mode-1-routes-to-summarize-and-not-lookup` is not
killed by this suite and cannot be today: `lookup` and `summarize` both answer
the same `NOT-FOUND-R0` sentinel at R0, so from outside the cell the two modes
are indistinguishable. It is left in the table with that note rather than
deleted, and it becomes killable the moment either mode returns an answer of
its own. A run reporting **4 killed, 1 survived** is the expected result; a
run reporting 5 killed means someone gave mode 1 an answer and should say so.

Note the exit status: `nbb tools/mutate.cljs` exits **1** while that survivor
is in the table, because a survivor is a finding about the suite. On this
repo today that `1` is the *expected* status, so do not wire this command into
a gate that reads a non-zero exit as breakage.

`tools/mutate.cljs` restores the file in a `finally` and on
SIGINT/SIGTERM/SIGHUP. That is best-effort by construction — `kill -9` runs no
user code. If a run is killed hard, check `git status` for a stray
<!-- check-docs:ignore-start naming the stray file you must look for; it must NOT exist -->
`src/isic/coordinator.cljc.orig`
<!-- check-docs:ignore-end -->
before doing anything else.

## Where things are

| path | what it is |
|---|---|
| `src/isic/coordinator.cljc` | the cell. Modes: 0 lookup, 1 summarize, others → `noop` |
| `test/isic/coordinator_test.cljc` | the portable suite, run by both runners above |
| `test/run_portable.cljs` | the nbb entry point |
| `tools/mutations.edn` | one mutation per invariant, with the survivor documented |
| `tools/mutate.cljs` | applies them, to prove the suite can fail |
| `tools/check-docs.cljs` | resolves every path these two docs name |
| `deps.edn` | the `:test` and `:lint` aliases |
| `manifest.edn` | actor metadata; `:pipelines` is empty at R0 |
| `identity.edn` | `did:web:isic.etzhayyim.com` (a compatibility identity) |
| `data/` | **empty at R0.** The ISIC Rev.4 hierarchy goes here |
| `lex/`, `wire/` | empty placeholders for EDN contracts and external payloads |

## What you cannot do yet

- **Look up an ISIC code.** `data/` holds only `.gitkeep`; `lookup` answers
  `NOT-FOUND-R0` by design, and the test suite asserts exactly that.
- **Run modes 2–6.** `coverage`/`parent`/`children`/`materialize`/`ratio` are
  named in the namespace docstring and in `CLAUDE.md`, mirroring
  `cloud-itonami/isco`. Only modes 0 and 1 are routed; everything else
  reaches `noop` deliberately and returns your state untouched. (They are
  *not* named in `manifest.edn`, whose `:pipelines` is empty.)
- **Deploy it.** `:pipelines` is `[]`. There is no serve path in this repo.

The next real step for this repo is ingesting UN ISIC Rev.4 into `data/`,
after which the coverage claims must be source-cited (charter gate in
`CLAUDE.md`) and the surviving mutation becomes killable.

## Keeping this file honest

Every path and command named above is checked by:

```sh
nbb tools/check-docs.cljs
```

It reads `README.md` and this file, resolves every repo path they name, and
refuses runners this workspace has retired. It exists because the previous
version of `README.md` told operators to run

<!-- check-docs:ignore-start quoting the pre-2026-09-06 README; these are the defects, not instructions -->
```sh
bb -cp src:test run_tests.clj    # File does not exist: run_tests.clj
```

and named `src/isic/coordinator.clj` and `dependencies.edn`, neither of which
is in the tree.
<!-- check-docs:ignore-end -->
Prose rots silently; a checker that goes red does not.
