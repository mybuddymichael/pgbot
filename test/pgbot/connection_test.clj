(ns pgbot.connection-test
  (:require [clojure.test :refer [deftest is]]
            pgbot.connection))

(defonce connection
  (#'pgbot.connection/create "irc.freenode.net" 6667 "pgbot"))
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

(deftest nick-is-optional
  (let [connection (#'pgbot.connection/create "irc.freenode.net" 6667)]
    (is (= nil (connection :nick)))
    (.close (connection :socket))))
