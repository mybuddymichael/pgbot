(ns pgbot.commands-test
  (:require [clojure.test :refer [deftest is]]
            [pgbot.commands :as commands]))

(deftest join-sends-an-appropriate-join-message
  (let [connection {:out (java.io.StringWriter.) :nick "pgbot"}]
    (commands/join connection "##pgbottest")
    (is (= "JOIN ##pgbottest\n"
           (str (connection :out))))))
