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
