(ns pgbot.io-test
  (:require [clojure.test :refer [deftest is use-fixtures]])
  (:use pgbot.io)
  (:import java.io.StringWriter))

(def ^:dynamic *connection*)

(defn connection-fixture [f]
  (binding [*connection* {:out (StringWriter.)}]
    (f)))

(use-fixtures :each connection-fixture)

(deftest send-message-writes-message-to-connection-out
  (send-message *connection* "Test string.")
  (is (= "Test string.\n"
         (str (*connection* :out)))))
