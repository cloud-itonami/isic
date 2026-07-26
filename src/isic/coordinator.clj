;; ISIC Industry Coordinator cell
;; kotoba-clj port — R0 scaffold (modes mirror com-etzhayyim-isco)
;; modes: 0=lookup 1=summarize 2=coverage 3=parent 4=children 5=materialize 6=ratio
;; Taxonomy ingest (UN ISIC Rev.4) pending — lookup returns NOT-FOUND-R0 until then.

(defn noop [state] state)

(defn lookup [state]
  ;; TODO: resolve ISIC code → industry record via kqe once taxonomy is ingested.
  (assoc state "result" "NOT-FOUND-R0"))

(defn summarize [state]
  ;; TODO: llm-infer "isic-summarizer" once data is present.
  (assoc state "result" "NOT-FOUND-R0"))

(defn coordinator [state]
  (case (get state "mode" 0)
    0 (lookup state)
    1 (summarize state)
    (noop state)))
