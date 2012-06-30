(ns pgbot.connection
  (:require [clojure.java.io :as io])
  (:import java.net.Socket))

(defn create [host port]
  (let [socket (Socket. host port)]
    {:socket socket
     :in (io/reader socket)
     :out (io/writer socket)}))
