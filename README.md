# ISIC Industry Coordinator

**Repository**: `cloud-itonami/isic`

Standalone actor repository for the **UN ISIC Rev.4** classification mirror:
industries across the 4-level hierarchy (section → division → group → class).

**Start here: [`docs/operator-quickstart.md`](docs/operator-quickstart.md).**
Every command in it has been run, and its output recorded, against the commit
it names.

## Status: R0 scaffold

Taxonomy ingest is pending — `data/` holds no hierarchy yet, so `lookup`
answers the `NOT-FOUND-R0` sentinel rather than resolving a code, and the test
suite asserts exactly that. Modes 0 (`lookup`) and 1 (`summarize`) are routed;
the remaining modes named in the sibling contract
(coverage/parent/children/materialize/ratio, mirroring `cloud-itonami/isco`)
reach `noop` and return your state untouched.

A clean test run therefore means **the scaffold's routing is measured**. It
does not mean this cell resolves ISIC codes.

## Layout

| path | what it is |
|---|---|
| `src/isic/coordinator.cljk` | the coordinator cell |
| `test/isic/coordinator_test.cljk` | the portable suite |
| `test/run_portable.cljk` | nbb entry point — runs the suite without a JVM |
| `tools/mutations.edn` | one mutation per invariant, with the survivor documented |
| `tools/mutate.cljk` | applies them, to prove the suite can fail |
| `tools/check-docs.cljk` | resolves every path these docs name |
| `deps.edn` | the `:test` and `:lint` aliases |
| `manifest.edn`, `identity.edn`, `README.edn` | canonical repository metadata |
| `data/` | **empty at R0** — the ISIC Rev.4 hierarchy goes here |
| `lex/` | canonical EDN API contracts (placeholder) |
| `wire/` | external JSON, JSON-LD, BPMN, sample payloads (placeholder) |

## Verifying it

```sh
nbb --classpath src:test test/run_portable.cljk   # portable suite, no JVM
clojure -M:test                                   # same suite on the JVM
clojure -M:lint                                   # clj-kondo, --fail-level error
nbb tools/check-docs.cljk                         # the docs point at real things
```

`docs/operator-quickstart.md` walks these with their expected output, and
covers the mutation harness — including the one mutation that survives on
purpose.

The coordinator is `.cljc` and contains no reader conditionals, so it runs
unchanged on nbb and on the JVM. This workspace's runtime order puts the JVM
last (ADR-2607173000), which is why the nbb runner is the primary one.

## Catalog and actor boundary

This repository owns industry lookup and materialization workflows. The
machine-readable source catalog is `cloud-itonami/org-un-isic`, while per-code
business blueprints live in the `cloud-itonami-isic-*` family, which consume
this repo as the canonical "what is ISIC code X" source. The historical ISIC
DID and protocol namespaces remain compatibility identities.

Do **not** migrate this actor into `cloud-itonami` — that inverts the
dependency (consumer → authority). See `CLAUDE.md` for the charter gates.

## Build artifacts

Generated WASM, shell build runners, and Go/TinyGo artifacts are intentionally
not repository assets. `deps.edn` pins only what the `:test` and `:lint`
aliases need; there is no kotoba engine dependency in this repo today.
