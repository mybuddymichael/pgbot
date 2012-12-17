(ns pgbot.connection
  "A collection of functions for creating and maintaining connections to
  IRC servers."
  (:require (clojure.java [io :as io]))
  (:import java.net.Socket))

(defn create
  "Create a connection and get a map containing information about it."
  [host port & [nick]]
  (let [socket (Socket. host (Integer. port))]
    {:socket socket
     :in (io/reader socket)
     :out (io/writer socket)
     :nick nick}))
