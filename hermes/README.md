# hermes/ — the resident bots that act for this repository

This directory is the **source of truth** for the Hermes profiles listed below
(ADR-2609241200). The host's `~/.hermes/profiles/<profile>` is materialized
from `hermes/profiles/<profile>/` and checked against it:

```
kbb --backend sci scripts/hermes-profile-repo.cljk materialize <profile>   # repo -> host
kbb --backend sci scripts/hermes-profile-repo.cljk check <profile>         # 0 agree / 1 drift / 2 could not compare
kbb --backend sci scripts/hermes-profile-repo.cljk export <profile>        # host -> repo, then commit
```

(run from the com-junkawasaki/root superproject; registry
`manifest/hermes-profile-repos.edn`.)

Each profile directory holds SOUL.md, profile.yaml, config.yaml (host-local
blocks removed), cron/jobs.json (definitions only), scripts/ and the skills the
profile owns. **Never here:** `.env` or any secret value, workspace/ledgers,
sessions, memories, logs, caches, run state.

## Profiles

| profile | description |
|---|---|
| `itonami-isic-a` | itonami ISIC major A Agriculture, forestry and fishing occupation bot |
| `itonami-isic-b` | itonami ISIC major B Mining and quarrying occupation bot |
| `itonami-isic-c` | itonami ISIC major C Manufacturing occupation bot |
| `itonami-isic-d` | itonami ISIC major D Electricity, gas, steam and air conditioning occupation bot |
| `itonami-isic-e` | itonami ISIC major E Water supply; sewerage, waste occupation bot |
| `itonami-isic-f` | itonami ISIC major F Construction occupation bot |
| `itonami-isic-g` | itonami ISIC major G Wholesale and retail trade occupation bot |
| `itonami-isic-h` | itonami ISIC major H Transport and storage occupation bot |
| `itonami-isic-i` | itonami ISIC major I Accommodation and food service occupation bot |
| `itonami-isic-j` | itonami ISIC major J Information and communication occupation bot |
| `itonami-isic-k` | itonami ISIC major K Financial and insurance occupation bot |
| `itonami-isic-l` | itonami ISIC major L Real estate occupation bot |
| `itonami-isic-m` | itonami ISIC major M Professional, scientific and technical activities occupation bot |
| `itonami-isic-n` | itonami ISIC major N Administrative and support occupation bot |
| `itonami-isic-o` | itonami ISIC major O Public administration and defence occupation bot |
| `itonami-isic-p` | itonami ISIC major P Education occupation bot |
| `itonami-isic-q` | itonami ISIC major Q Human health and social work occupation bot |
| `itonami-isic-r` | itonami ISIC major R Arts, entertainment and recreation occupation bot |
| `itonami-isic-s` | itonami ISIC major S Other service activities occupation bot |
| `itonami-isic-t` | itonami ISIC major T Households as employers occupation bot |
| `itonami-isic-u` | itonami ISIC major U Extraterritorial organisations and bodies occupation bot |
