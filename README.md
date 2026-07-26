# ISIC Industry Coordinator

Standalone actor repository for the complete **UN ISIC Rev.4** classification
mirror: industries across the 4-level hierarchy (section → division →
group → class).

- `manifest.edn`, `identity.edn`, `dependencies.edn`: canonical repository metadata
- `data/`: authoritative industry hierarchy (ISIC Rev.4 — ingest pending, R0)
- `lex/`: canonical EDN API contracts
- `wire/`: external JSON, JSON-LD, BPMN, and sample payloads
- `src/isic/coordinator.clj`: kotoba-clj coordinator source

## Authority, not duplicate

This is the **taxonomy authority mirror** (sibling of `isco`/`unspsc`/`gtin`).
`cloud-itonami`'s per-code business blueprints (`cloud-itonami-isic-*`) consume
it; migrating it to cloud-itonami would invert the dependency.
See ADR-2607261601 §4(a).

## Status

**R0 scaffold** — taxonomy ingest (ISIC Rev.4 source data into `data/`) is
pending. Coordinator modes mirror `isco` (lookup/summarize/coverage/parent/
children/materialize/ratio).

Run the deterministic, network-free suite with:

```sh
bb -cp src:test run_tests.clj
```

Generated WASM, shell build runners, and Go/TinyGo artifacts are intentionally
not repository assets. The external kotoba engine is pinned in `dependencies.edn`.
