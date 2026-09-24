;; itonami-isic-c (Manufacturing) business bot — decision-free collector.
;; Classifies each member blueprint actor by maturity from its own README and
;; reports the kotoba-native (amu check --jvm-free) readiness of the frontier
;; actor.  The agent reads SCANNED + member list and reports ONE finding.
;; Run: nbb scripts/isicC_evidence.cljs

(require '[clojure.string :as str]
         '["node:child_process" :as cp]
         '["node:fs" :as fs]
         '["node:path" :as path])

(def root "~/github/com-junkawasaki")
(def org "orgs/cloud-itonami")
(def repo-prefix "cloud-itonami-isic-")

;; Manufacturing member blueprint repos (135).
(def members
  [1010 1020 1030 1040 1050 1061 1062 1071 1072 1073 1074 1075 1079 1080 1101 1102 1103 1104 1200 1311 1312 1313 1391 1392 1393 1394 1399 1410 1420 1430 1511 1512 1520 1610 1621 1622 1623 1629 1701 1702 1709 1811 1812 1820 1910 1920 2011 2012 2013 2021 2022 2023 2029 2030 2100 2211 2219 2220 2310 2391 2392 2393 2394 2395 2396 2399 2410 2420 2431 2432 2511 2512 2513 2591 2592 2593 2599 2610 2620 2630 2640 2651 2652 2660 2670 2680 2710 2720 2731 2732 2733 2740 2750 2790 2811 2812 2813 2814 2815 2816 2817 2818 2819 2821 2822 2823 2824 2825 2826 2829 2910 2920 2930 3011 3012 3020 3030 3091 3092 3099 3100 3211 3212 3220 3230 3240 3250 3290 3311 3312 3313 3314 3315 3319 3320])

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
