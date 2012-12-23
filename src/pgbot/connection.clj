(ns pgbot.connection
  "A collection of functions for creating and maintaining connections to
  IRC servers."
  (:require pgbot.io))

(defn- create
  "Create a connection and get a map containing information about it."
  [host port nick]
  (let [socket (java.net.Socket. host (Integer. port))]
    {:socket socket
     :in (clojure.java.io/reader socket)
     :out (clojure.java.io/writer socket)
     :nick nick}))

(defn- register
  "Send a 'handshake' message to register the connection."
  [connection]
  (let [nick (connection :nick)]
    (pgbot.io/send-message connection "USER" nick "i *" nick)))
