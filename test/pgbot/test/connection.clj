(ns pgbot.test.connection
  (:require [clojure.test :refer [deftest is]]
            [pgbot.connection :as connection])) 

(deftest create-returns-a-map-with-socket-in-and-out
  (let [irc (connection/create "irc.freenode.net" 6665)]
    (is (= [true true true]
           (map #(contains? irc %) [:socket :in :out])))))
