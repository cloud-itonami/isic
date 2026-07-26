# com-etzhayyim-isic

ISIC Industry Coordinator — UN ISIC Rev.4 classification mirror actor.

## Authority boundary (ADR-2607261601 §4a)

This actor is a **taxonomy authority mirror** (sibling of `isco`/`unspsc`/
`gtin`/`apqc`/`masago`). `cloud-itonami`'s per-code business blueprints
(`cloud-itonami-isic-*`, 457 repos) consume it as the canonical "what is
ISIC code X" source.

- **Do NOT migrate to cloud-itonami.** That would invert the dependency
  (consumer → authority). cloud-itonami-gtin-catalog's README explicitly
  states it is the "OSS-operator counterpart to com-etzhayyim-gtin".
- This actor does **not** run per-industry business operations; it owns
  only the taxonomy mirror + lookup/summarize/coverage.

## Status: R0 scaffold

- Coordinator modes mirror `isco` (lookup/summarize/coverage/parent/children/
  materialize/ratio).
- Taxonomy ingest (ISIC Rev.4 source → `data/isic-industries.edn`) is pending.
- Pipelines (weekly social, occupation subscribe, health xrpc) will be added
  once the hierarchy is materialized.

## Charter gates

- G1: stays a mirror — does not name/rank specific real-world firms per code.
- Coverage claims must be source-cited (UN ISIC Rev.4 publication).
