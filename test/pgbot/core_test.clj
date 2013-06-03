(ns pgbot.core-test
  (:require [clojure.test :use [is]]
            [pgbot.test-helpers :use [deftest* deftest-*]]
            pgbot.core))

(deftest* "events is a map of keywords to fns"
  (is (= [true true true]
         (map fn? (#'pgbot.core/events :incoming))))
  (is (= [true true true]
         (map fn? (#'pgbot.core/events :outgoing)))))
