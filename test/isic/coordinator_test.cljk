(ns isic.coordinator-test
  (:require [clojure.test :refer [deftest is testing]]
            [isic.coordinator :as c]))

(deftest noop-test
  (testing "noop is identity"
    (is (= {:a 1} (c/noop {:a 1})))))

(deftest lookup-r0-test
  (testing "lookup returns NOT-FOUND-R0 until taxonomy ingest"
    (is (= "NOT-FOUND-R0" (get (c/lookup {}) "result")))))

(deftest coordinator-default-mode-test
  (testing "default mode 0 routes to lookup"
    (is (= "NOT-FOUND-R0" (get (c/coordinator {}) "result")))))

(deftest coordinator-unknown-mode-test
  (testing "unknown mode routes to noop"
    (is (= {:mode 99 :x 1} (c/coordinator {:mode 99 :x 1})))))


;; ---------------------------------------------------------------------------
;; Added with the 2026-08-18 `.clj` → `.cljc` conversion.
;;
;; The four tests above were the whole suite, and between them they left the
;; string mode key, the summarize branch and the state-passthrough of the
;; lookup modes unmeasured. Measured, not guessed: mutations written against
;; the source before these tests existed showed it.
;; ---------------------------------------------------------------------------

(deftest the-mode-is-read-under-both-key-conventions
  (testing "this cell is driven from hand-written EDN (`:mode`) and from
            JSON-shaped state crossing a wire (\"mode\"). A caller using
            either convention must reach the mode it named — being silently
            demoted to 0 would look exactly like a successful lookup"
    (is (= {"mode" 99 :x 1} (c/coordinator {"mode" 99 :x 1}))
        "string key, unimplemented mode -> untouched")
    (is (= {:mode 99 :x 1} (c/coordinator {:mode 99 :x 1}))
        "keyword key, unimplemented mode -> untouched"))
  (testing "and the string key WINS when both are present, which is the
            documented precedence rather than an accident of `get`"
    (is (= {"mode" 99 :mode 0} (c/coordinator {"mode" 99 :mode 0})))))

(deftest noop-returns-the-identical-state
  (testing "`noop` must hand back what it was given — not an equal map it
            rebuilt. `(assoc state \"result\" …)` on an unrecognised mode
            would make 'I do not know this mode' indistinguishable from
            'I looked and found nothing', which is the one confusion an R0
            scaffold cannot afford"
    (let [state {:mode 7 :payload [1 2 3]}]
      (is (identical? state (c/noop state)))
      (is (identical? state (c/coordinator state))
          "routing through the default branch preserves identity too"))))

(deftest implemented-modes-preserve-the-rest-of-the-state
  (testing "lookup and summarize ADD a result; they do not replace the state"
    (is (= {"mode" 0 :carried :through "result" "NOT-FOUND-R0"}
           (c/coordinator {"mode" 0 :carried :through})))
    (is (= {"mode" 1 :carried :through "result" "NOT-FOUND-R0"}
           (c/coordinator {"mode" 1 :carried :through})))))

;; ⚠ RECORDED, NOT FIXED. A mutation routing mode 1 to `lookup` SURVIVES
;; this suite, and no test here can kill it: `lookup` and `summarize` return
;; the SAME sentinel at R0, so the two modes are today indistinguishable from
;; outside. Writing a test that reached inside to tell them apart would
;; measure the implementation rather than the contract, and deleting the
;; mutation would hide a real property of the scaffold. It stays in
;; `tools/mutations.edn` as a survivor with this note, and it will start
;; being killable the moment either mode returns something of its own.
(deftest mode-1-reaches-summarize-even-though-the-answer-is-shared
  (testing "what CAN be asserted today: mode 1 is routed and answered, so
            the branch is reached rather than falling through to noop"
    (is (contains? (c/coordinator {"mode" 1}) "result"))))

(deftest a-state-of-nil-is-not-mistaken-for-an-empty-lookup
  (testing "`(get nil \"mode\" …)` is 0, so nil routes to `lookup`, and
            `(assoc nil …)` yields a one-key map. That is the behaviour;
            it is asserted so a caller passing nothing can see what it
            gets rather than discovering it"
    (is (= {"result" "NOT-FOUND-R0"} (c/coordinator nil)))))
