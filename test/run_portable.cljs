#!/usr/bin/env nbb
;; The portable suite on nbb — no build step, no JVM.
;;
;; This file is the whole point of the `.cljc` conversion. A reader
;; conditional whose `:cljs` branch nothing ever evaluates is not
;; portability, it is the appearance of it, and this namespace's claim is
;; stronger than most: it has NO reader conditionals, so "it is portable"
;; rests entirely on something having actually loaded and run it under
;; ClojureScript. That is this file.
;;
;;   nbb --classpath src:test test/run_portable.cljs
;;
;; Run it from somewhere that is NOT this repo, too. This namespace touches
;; no file at runtime, so it must not care what the process's working
;; directory is — and the only way to know that is to run it from elsewhere:
;;
;;   cd /tmp && nbb --classpath <repo>/src:<repo>/test <repo>/test/run_portable.cljs
;;
;; Every `deftest`-bearing portable namespace must be named BOTH in the
;; require and in `run-tests`: requiring registers the vars, only
;; `run-tests` runs them, and a runner naming a subset prints the same
;; `Ran N tests` shape as one naming all of them.
(require '[cljs.test :as t]
         '[isic.coordinator-test])

(defmethod t/report [:cljs.test/default :end-run-tests] [m]
  (when-not (t/successful? m) (set! (.-exitCode js/process) 1)))

(t/run-tests 'isic.coordinator-test)
