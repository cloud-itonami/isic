#!/usr/bin/env python3
"""Measurements for the itonami-isic-g bot (Wholesale and retail trade).

Decision-free. Classifies the member blueprint actors and reports kotoba-native
compile status of the frontier actor via amu check --jvm-free.  The measuring
lives in isicG_evidence.cljs (nbb); this wrapper exists because Hermes cron
executes --script files as bash or python, not nbb.  REFUSED banner on failure.
Throttled; guarded by the shared cron-throttle state.
"""
import os
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from throttle import gate, mark  # noqa: E402

NBB = os.environ.get("HYAKKA_NBB", "/opt/homebrew/bin/nbb")
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

JOB = "itonami-isic-g"
COOLDOWN_H = float(os.environ.get("ISICG_COOLDOWN_HOURS", "24"))


def refuse(why):
    print("REFUSED — no evidence was gathered this run.")
    print(why)
    print()
    print("Do not propose anything. A migration proposal built on an unread "
          "tree is a proposal built on nothing. Report this refusal and stop.")
    sys.exit(0)


def main():
    gate(JOB, hours=COOLDOWN_H)
    proc = subprocess.run(
        [NBB, os.path.join(SCRIPT_DIR, "isicG_evidence.cljs")],
        capture_output=True, text=True, timeout=600)
    if proc.returncode == 2:
        refuse("the evidence collector refused:\n" + proc.stderr.strip()[:800])
    if proc.returncode != 0:
        refuse(f"the evidence collector exited {proc.returncode}:\n"
               + (proc.stderr.strip() or proc.stdout.strip())[:800])
    if "SCANNED" not in proc.stdout:
        refuse("the collector produced no SCANNED line")
    mark(JOB)
    print(proc.stdout.strip())


if __name__ == "__main__":
    main()
