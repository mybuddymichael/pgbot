(ns pgbot.connection
  (:require [pgbot.messages :use [parse compose]]))

(defn create
  "Opens a connection to a server. Returns a map containing information
   about the connection."
  [host port nick channel]
  (let [socket (java.net.Socket. host (Integer. port))]
    {:socket socket
     :in (clojure.java.io/reader socket)
     :out (clojure.java.io/writer socket)
     :nick nick
     :channel channel}))

(defn get-line
  "Grabs a single line from the connection, parsing it into a message
   map, or returning nil if the socket is closed."
  [connection]
  (try (->> (connection :in) .readLine parse)
    (catch java.net.SocketException _ nil)))

(defn send-message
  "Sends one or more messages through a connection's writer."
  [connection & messages]
  (binding [*out* (connection :out)]
    (doseq [m messages]
      (println (compose m)))))
