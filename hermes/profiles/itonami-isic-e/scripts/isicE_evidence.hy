#!/usr/bin/env hy
"Measurements for the itonami-isic-e bot (Water supply; sewerage, waste).

Decision-free. Classifies the member blueprint actors and reports kotoba-native
compile status of the frontier actor via amu check --jvm-free.  The measuring
lives in isicE_evidence.cljs (nbb); this wrapper keeps the Hermes entrypoint in Hy and delegates evidence collection
to nbb.  REFUSED banner on failure.
Throttled; guarded by the shared cron-throttle state.
"
(import os)
(import subprocess)
(import sys)
((. sys.path insert) 0
  ((. os.path dirname) ((. os.path abspath) __file__)))
(import throttle [gate mark])
(setv NBB ((. os.environ get) "HYAKKA_NBB" "/opt/homebrew/bin/nbb"))
(setv SCRIPT_DIR
  ((. os.path dirname) ((. os.path abspath) __file__)))
(setv JOB "itonami-isic-e")
(setv COOLDOWN_H (float ((. os.environ get) "ISICE_COOLDOWN_HOURS" "24")))

(defn refuse [why]
  (print "REFUSED — no evidence was gathered this run.")
  (print why)
  (print)
  (print "Do not propose anything. A migration proposal built on an unread tree is a proposal built on nothing. Report this refusal and stop.")
  (sys.exit 0))

(defn main []
  (gate JOB :hours COOLDOWN_H)
  (setv proc
    (subprocess.run [NBB ((. os.path join) SCRIPT_DIR "isicE_evidence.cljs")]
      :capture_output True
      :text True
      :timeout 600))
  (when (= proc.returncode 2)
    (refuse (+ "the evidence collector refused:\n" (cut ((. proc.stderr strip)) 800))))
  (when (!= proc.returncode 0)
    (refuse
      (+
        f"the evidence collector exited {proc.returncode}:\n"
        (cut
          (or ((. proc.stderr strip)) ((. proc.stdout strip)))
          800))))
  (when (not-in "SCANNED" proc.stdout) (refuse "the collector produced no SCANNED line"))
  (mark JOB)
  (print ((. proc.stdout strip))))

(when (= __name__ "__main__") (main))