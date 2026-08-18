(ns isic.coordinator
  "ISIC Industry Coordinator cell — R0 scaffold (modes mirror com-etzhayyim-isco).

  modes: 0=lookup 1=summarize 2=coverage 3=parent 4=children 5=materialize 6=ratio
  Taxonomy ingest (UN ISIC Rev.4) is pending, so `lookup` answers
  `NOT-FOUND-R0` rather than resolving a code.

  ## Why this is `.cljc` and not `.clj`

  Nothing in here ever needed the JVM: no interop, no `io/resource`, no
  `slurp`. It was `.clj` by scaffolding habit, and that habit pinned every
  consumer of this cell to the runtime this workspace puts LAST — the order
  is kotoba-wasm → clojurewasm → ClojureScript → nbb, with the JVM below
  all of them. A `case` over an integer has no business choosing a runtime.

  The conversion is therefore the file extension and nothing else: there is
  not one reader conditional in this namespace, because there is not one
  construct that differs between the two. `test/run_portable.cljs` is what
  makes that a measurement instead of a claim.")

(defn noop
  "The identity mode. Every mode this scaffold has not implemented routes
  here, and it must hand the state back UNCHANGED — a coordinator that
  quietly stamped a `result` on an unrecognised mode would make 'I do not
  know this mode' indistinguishable from 'I looked and found nothing'."
  [state]
  state)

(defn lookup [state]
  ;; TODO: resolve ISIC code → industry record via kqe once taxonomy is ingested.
  (assoc state "result" "NOT-FOUND-R0"))

(defn summarize [state]
  ;; TODO: llm-infer "isic-summarizer" once data is present.
  (assoc state "result" "NOT-FOUND-R0"))

(defn coordinator
  "Route `state` by its mode.

  The mode is read under the STRING key first and the KEYWORD key second,
  because this cell is driven both from EDN written by hand (`:mode`) and
  from JSON-shaped state crossing a wire (`\"mode\"`), and a caller using
  the other convention must not be silently treated as mode 0."
  [state]
  (case (get state "mode" (get state :mode 0))
    0 (lookup state)
    1 (summarize state)
    (noop state)))
