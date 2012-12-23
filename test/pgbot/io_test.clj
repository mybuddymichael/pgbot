(ns pgbot.io-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            pgbot.io))

(def ^:dynamic *connection*)

(defn connection-fixture [f]
  (binding [*connection* {:out (java.io.StringWriter.)}]
    (f)))

(use-fixtures :each connection-fixture)

(deftest send-message-writes-message-to-connection-out
  (pgbot.io/send-message *connection* "Test string.")
  (is (= "Test string.\n"
         (str (*connection* :out)))))

(deftest send-message-concatenates-multiple-arguments-with-spaces
  (pgbot.io/send-message *connection* "This" "and" "that.")
  (is (= "This and that.\n"
         (str (*connection* :out)))))
