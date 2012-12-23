(ns pgbot.connection
  "A collection of functions for creating and maintaining connections to
  IRC servers."
  (:import java.net.Socket))

(defn- create
  "Create a connection and get a map containing information about it."
  [host port & [nick]]
  (let [socket (Socket. host (Integer. port))]
    {:socket socket
     :in (clojure.java.io/reader socket)
     :out (clojure.java.io/writer socket)
     :nick nick}))
