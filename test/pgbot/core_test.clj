(ns pgbot.core-test
  (:require [clojure.test :refer [deftest is]]
            pgbot.core))

(defonce connection
  (#'pgbot.core/create-connection "irc.freenode.net" 6667 "pgbot"))
(.close (connection :socket))

(deftest create-returns-a-map-with-socket-in-out-and-nick
  (is (= [true true true true]
         (map #(contains? connection %) [:socket :in :out :nick]))))

(deftest connection-has-a-socket
  (is (= java.net.Socket
         (class (connection :socket)))))

(deftest connection-has-a-reader
  (is (= java.io.BufferedReader
         (class (connection :in)))))

(deftest connection-has-a-writer
  (is (= java.io.BufferedWriter
         (class (connection :out)))))

(deftest connection-has-a-nick
  (is (= "pgbot"
         (connection :nick))))

(deftest send-message-writes-message-to-connection-out
  (let [connection {:out (java.io.StringWriter.)}]
    (#'pgbot.core/send-message connection "Test string.")
    (is (= "Test string.\n"
           (str (connection :out))))))

(deftest send-message-concatenates-multiple-arguments-with-spaces
  (let [connection {:out (java.io.StringWriter.)}]
    (#'pgbot.core/send-message connection "This" "and" "that.")
    (is (= "This and that.\n"
           (str (connection :out))))))

(deftest register-connection-sends-the-appropriate-handshake-messages
  (let [connection {:out (java.io.StringWriter.)
                    :nick "pgbot"}]
    (#'pgbot.core/register-connection connection)
    (is (= "NICK pgbot\nUSER pgbot i * pgbot\n"
           (str (connection :out))))))

(deftest read-line-from-connection-gets-a-single-line
  (let [connection
        (#'pgbot.core/create-connection "irc.freenode.net" 6667 "pgbot")]
    (is (string? (#'pgbot.core/read-line-from-connection connection)))
    (.close (connection :socket))))

(deftest read-line-from-connection-returns-nil-if-socket-is-closed
  (let [connection
        (#'pgbot.core/create-connection "irc.freenode.net" 6667 "pgbot")]
    (.close (connection :socket))
    (is (nil? (#'pgbot.core/read-line-from-connection connection)))))
