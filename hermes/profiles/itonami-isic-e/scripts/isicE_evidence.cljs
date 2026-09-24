;; itonami-isic-e (Water supply; sewerage, waste) business bot — decision-free collector.
;; Classifies each member blueprint actor by maturity from its own README and
;; reports the kotoba-native (amu check --jvm-free) readiness of the frontier
;; actor.  The agent reads SCANNED + member list and reports ONE finding.
;; Run: nbb scripts/isicE_evidence.cljs

(require '[clojure.string :as str]
         '["node:child_process" :as cp]
         '["node:fs" :as fs]
         '["node:path" :as path])

(def root "~/github/com-junkawasaki")
(def org "orgs/cloud-itonami")
(def repo-prefix "cloud-itonami-isic-")

;; Water supply; sewerage, waste member blueprint repos (8).
(def members
  [3600 3700 3811 3812 3821 3822 3830 3900])

(defn repo-dir [code]
  (path/join root org (str repo-prefix code)))

(defn classify-maturity [code]
  (let [p (path/join (repo-dir code) "README.md")]
    (when (fs/existsSync p)
      (let [t (str (fs/readFileSync p "utf8"))]
        (cond
          (boolean (re-find #":implemented" t)) "implemented"
          (boolean (re-find #"R0|Status: R0|scaffold" t)) "scaffold"
          :else "present")))))

(defn find-count [dir matcher]
  (try
    (let [cmd (str "find " dir " -type f -name " matcher)
          out (str (cp/execSync cmd #js {:timeout 60000}))]
      (count (remove empty? (str/split out "\n"))))
    (catch :default _ 0)))

(defn lang-counts [code]
  (let [dir (repo-dir code)]
    (if (not (fs/existsSync dir))
      {:kotoba -1 :clj -1}
      {:kotoba (find-count dir "*.kotoba")
        :clj (+ (find-count dir "*.clj")
                (find-count dir "*.cljc")
                (find-count dir "*.cljs"))})))

(defn -main []
  (let [rows (map (fn [c]
                    (let [m (or (classify-maturity c) "unmeasured")
                          lc (lang-counts c)]
                      [c m (:kotoba lc) (:clj lc)]))
                  members)
        implemented (filter (fn [[c m _x _y]] (= m "implemented")) rows)
        kt (filter (fn [[c _x k _y]] (pos? k)) rows)
        mig (sort-by (fn [[_x _y _z c]] (if (neg? c) 100000 c))
                     (filter (fn [[c _x k cl]] (and (not (pos? k)) (pos? cl))) rows))
        frontier-row (first mig)]
    (println (str "SCANNED members=" (count rows)
                  ";implemented=" (count implemented)
                  ";kotoba-native=" (count kt)
                  ";migration-candidates=" (count mig)
                  ";frontier=" (if frontier-row (nth frontier-row 0) "none")))
    (doseq [[c m k cl] rows]
      (println (str "member\t" c "\t" m "\tkotoba=" k "\tclj=" cl)))
    (when frontier-row
      (println (str "NEXT-ACTION-RECOMMENDED " (nth frontier-row 0)
                    " — smallest remaining JVM actor (clj=" (nth frontier-row 3)
                    "); migrate its smallest vertical slice to .kotoba and gate with amu check --jvm-free per SOUL.md")))))

(-main)
