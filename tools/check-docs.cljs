(ns check-docs
  "Resolve every repo-relative path the docs name, and refuse runners this
  workspace has retired.

  ## Why this exists

  Until 2026-09-06 `README.md` was the only entry point this repo had, and
  every instruction in it was wrong: it prescribed a runner that is retired,
  pointed at a test file that is not in the tree, named the coordinator with
  the wrong extension, and referred twice to a `dependencies.edn` that does
  not exist. None of it was caught, because prose has no exit status. The
  suite next door proves it can fail (`tools/mutate.cljs`); the docs could
  not. The retired runner was even still on PATH, so the documented command
  got far enough to fail with `File does not exist`, which reads like a
  broken checkout rather than a broken README.

  ## What it checks, and what it does NOT

  Two things a machine can settle:

  1. every backtick-quoted token that is a REPO-RELATIVE path resolves;
  2. no fenced command line invokes a retired runner.

  It does NOT check that the documented commands still produce the documented
  output. That needs running them, which is `docs/operator-quickstart.md`'s
  job. **A green run here means the docs point at things that exist. It does
  not mean the docs are true.**

  ## Two ways a token is not a path claim

  Intrinsic (not exemptions — these tokens never asserted a path):

  - `<org>/<repo>` identifiers, globs, bare extensions, URLs, DIDs. The rule
    is that the first segment must be a real top-level entry of THIS repo,
    which is why `cloud-itonami/isic` is skipped and `src/isic/x.clj` is not.

  Explicit, for docs that must name something absent — a counterexample, or a
  stray file you are told to look for:

      <!-- check-docs:ignore-start quoting the pre-2026-09-06 README -->
      ...
      <!-- check-docs:ignore-end -->

  Exempted regions and their reasons are PRINTED on every run, including
  clean ones. An exemption nobody can see is a hole; one that is read out
  every time is a claim someone can argue with.

  Exit: 0 clean / 1 findings / 2 refused to answer."
  (:require ["node:fs" :as fs]
            [clojure.string :as str]))

(def docs
  "The files an operator is told to read. Both must exist — dropping one and
  reporting clean on the other is the failure this whole file is about."
  ["README.md" "docs/operator-quickstart.md"])

(def retired
  "Runner -> why. `bb` was retired as a script host by ADR-2607173000; this
  workspace's order is kotoba-wasm -> clojurewasm -> ClojureScript -> nbb,
  with the JVM below all of them."
  {"bb" "babashka is retired as a script host (ADR-2607173000) — use nbb"})

(def top-level
  "Top-level entries of this repo. A token's first segment must be one of
  these for the token to be a claim about a path in THIS tree."
  (delay (set (.readdirSync fs "."))))

(def ^:private ignore-open "<!-- check-docs:ignore-start ")
(def ^:private ignore-close "<!-- check-docs:ignore-end -->")

(defn- strip-ignored
  "Cut `check-docs:ignore` regions out of `text`. Returns `[text reasons]`.

  ## Why this scans strings instead of using a regex

  The obvious spelling is one `(?s)` pattern and `str/replace`. It does not
  work, and it fails in the direction that hides the failure.

  ClojureScript's reader hoists `(?s)` out of the pattern into the RegExp's
  `s` FLAG — `#\"(?s)a(.*?)b\"` has source `a(.*?)b` and flags `s`.
  `clojure.string/replace` then rebuilds the RegExp from `.source`, carrying
  only `g`, `i` and `m`. `s` is dropped, `.` stops crossing newlines, and a
  multi-line region silently matches nothing:

      (re-seq re \"a\\nb\")        ;=> ([\"a\\nb\" \"\\n\"])   matches
      (str/replace \"a\\nb\" re \"X\") ;=> \"a\\nb\"          replaces nothing

  Measured here on 2026-09-06. The first version of this file had exactly
  that bug: `re-seq` found the regions, so the run PRINTED `2 exempted
  region(s)` while stripping neither — it reported honouring exemptions it
  had not honoured. An unmeasured check returning the value of a measured
  one, which is the shape the rest of this repo's tooling exists to refuse.

  Two markers and `indexOf` cannot express the bug."
  [text]
  (loop [rest-text text, acc "", reasons []]
    (let [i (.indexOf rest-text ignore-open)]
      (if (neg? i)
        [(str acc rest-text) reasons]
        (let [after (subs rest-text (+ i (count ignore-open)))
              r-end (.indexOf after "-->")
              c (.indexOf after ignore-close)
              nxt (.indexOf after ignore-open)]
          (if (or (neg? r-end) (neg? c) (and (not (neg? nxt)) (< nxt c)))
            ;; Refuse rather than guess. Two ways this goes wrong, and the
            ;; second one is why the `nxt` test is here:
            ;;
            ;; 1. No close marker at all after an open — the rest of the file
            ;;    would become exempt.
            ;; 2. The NEXT open comes before the next close, i.e. someone
            ;;    deleted a close in the middle. A scan that just took the
            ;;    following close would swallow everything between two
            ;;    regions AND the marker of the second one, then report
            ;;    clean with fewer references and one fewer exemption.
            ;;
            ;; Measured 2026-09-06: case 2 actually happened to this file's
            ;; first version. Deleting one `ignore-end` took the scan from 37
            ;; path references to 21 and still exited 0 — a check that had
            ;; stopped looking, reporting the same value as one that looked
            ;; and found nothing.
            (do (println "REFUSED\tunterminated or overlapping check-docs:ignore region")
                (js/process.exit 2))
            (recur (subs after (+ c (count ignore-close)))
                   (str acc (subs rest-text 0 i))
                   (conj reasons (str/trim (subs after 0 r-end))))))))))

(defn- path-claim?
  "Is this backtick token asserting that a path exists in this repo?"
  [t]
  (and (not (str/includes? t " "))
       (not (str/includes? t "*"))
       (not (re-find #"^https?:|^did:" t))
       (if (str/includes? t "/")
         (contains? @top-level (first (str/split t #"/")))
         ;; no slash: a bare filename, which must have a stem before the
         ;; extension — `.cljc` used as prose is not a claim, `deps.edn` is
         (boolean (re-find #"^[^.].*\.(clj|cljc|cljs|edn|md)$" t)))))

(defn -main [& _]
  (let [missing (remove #(.existsSync fs %) docs)]
    (when (seq missing)
      ;; Refuse before reporting: if a doc we are told to read is gone, the
      ;; other one being clean says nothing about the repo's entry point.
      (println (str "REFUSED\tmissing doc: " (str/join ", " missing)))
      (js/process.exit 2))
    (let [findings (atom [])
          checked (atom 0)
          exemptions (atom [])]
      (doseq [f docs]
        (let [[text reasons] (strip-ignored (.readFileSync fs f "utf8"))]
          (doseq [r reasons] (swap! exemptions conj [f r]))
          (doseq [block (map second (re-seq #"(?s)```[a-z]*\n(.*?)```" text))
                  line (str/split-lines block)]
            (let [head (first (str/split (str/trim line) #"\s+"))]
              (when-let [why (get retired head)]
                (swap! findings conj [f (str "retired runner `" head "`: " why)
                                      (str/trim line)]))))
          (doseq [tok (map second (re-seq #"`([^`\n]+)`" text))]
            (when (path-claim? tok)
              (swap! checked inc)
              (when-not (.existsSync fs (str/replace tok #"/$" ""))
                (swap! findings conj [f (str "path does not resolve: " tok) ""]))))))
      (println (str "SCANNED\t" (count docs) " docs, " @checked " path references, "
                    (count @exemptions) " exempted region(s)"))
      (doseq [[f r] @exemptions]
        (println (str "  exempt\t" f " — " r)))
      ;; Evidence floor. A regex that stopped matching would otherwise report
      ;; "clean" — an unmeasured check returning the value of a measured,
      ;; passing one, which is the shape this repo's tooling exists to refuse.
      (when (zero? @checked)
        (println "no path references found — refusing to report a pass")
        (js/process.exit 2))
      (doseq [[f msg ctx] @findings]
        (println (str "  " f " — " msg))
        (when (seq ctx) (println (str "      " ctx))))
      (println (if (seq @findings)
                 (str (count @findings) " finding(s)")
                 "every documented path resolves; no retired runners"))
      (js/process.exit (if (seq @findings) 1 0)))))

(apply -main *command-line-args*)
