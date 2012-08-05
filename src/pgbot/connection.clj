(ns pgbot.connection
  (:require [clojure.java.io :as io])
  (:import java.net.Socket))

(defn create
  "Given a host and a port, generate an IRC connection map, containing
  pairs for the socket, the reader, and the writer."
  [host port]
  (let [socket (Socket. host port)]
    {:socket socket
     :in (io/reader socket)
     :out (io/writer socket)}))
