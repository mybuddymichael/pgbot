(ns pgbot.io-test
  (:require [clojure.test :refer [deftest is]])
  (:use pgbot.io)
  (:import java.io.StringWriter))

(defonce connection {:out (StringWriter.)})

(deftest send-message-writes-message-to-connection-out
  (send-message connection "Test string.")
  (is (= "Test string.\n"
         (str (connection :out)))))
