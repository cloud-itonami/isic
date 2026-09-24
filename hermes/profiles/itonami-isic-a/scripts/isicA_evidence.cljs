;; itonami-isic-a (Agriculture, forestry and fishing) business bot — decision-free collector.
;; Classifies each member blueprint actor by maturity from its own README and
;; reports the kotoba-native (amu check --jvm-free) readiness of the frontier
;; actor.  The agent reads SCANNED + member list and reports ONE finding.
;; Run: kbb --backend sci scripts/isicA_evidence.cljs
;; Fix 2026-09-13: ISIC codes are 4-digit and repo dirs are zero-padded
;; (cloud-itonami-isic-0111). The old (str repo-prefix code) built
;; "cloud-itonami-isic-111", which never matched on-disk dirs, so every member
;; measured as unmeasured/-1 and frontier=none was a measurement failure.

(require '[clojure.string :as str]
         '["node:child_process" :as cp]
         '["node:fs" :as fs]
         '["node:path" :as path])

(def root "~/github/com-junkawasaki")
(def org "orgs/cloud-itonami")
(def repo-prefix "cloud-itonami-isic-")

;; Agriculture, forestry and fishing member blueprint repos (38).
(def members
  [111 112 113 114 115 116 119 121 122 123 124 125 126 127 128 129 130 141 142 143 144 145 146 149 150 161 162 163 164 170 210 220 230 240 311 312 321 322])

;; ISIC codes are 4-digit; on-disk dirs are zero-padded.
(defn pad4 [n]
  (let [s (str n)
        z (str "0000" s)]
    (subs z (- (count z) 4))))

(defn repo-dir [code]
  (path/join root org (str repo-prefix (pad4 code))))

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
