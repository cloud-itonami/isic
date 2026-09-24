;; itonami-isic-g (Wholesale and retail trade) business bot — decision-free collector.
;; Classifies each member blueprint actor by maturity from its own README and
;; reports the kotoba-native (amu check --jvm-free) readiness of the frontier
;; actor.  The agent reads SCANNED + member list and reports ONE finding.
;; Run: nbb scripts/isicG_evidence.cljs

(require '[clojure.string :as str]
         '["node:child_process" :as cp]
         '["node:fs" :as fs]
         '["node:path" :as path])

(def root "~/github/com-junkawasaki")
(def org "orgs/cloud-itonami")
(def repo-prefix "cloud-itonami-isic-")

;; Wholesale and retail trade member blueprint repos (44).
(def members
  [4510 4520 4530 4540 4610 4620 4630 4641 4649 4651 4652 4653 4659 4661 4662 4663 4669 4671 4690 4711 4719 4721 4722 4723 4730 4741 4742 4751 4752 4753 4759 4761 4762 4763 4764 4771 4772 4773 4774 4781 4782 4789 4791 4799])

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
