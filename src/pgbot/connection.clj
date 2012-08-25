(ns pgbot.connection
  "Create and handle IRC connections."
  (:require (clojure [string :as string])
            (clojure.java [io :as io]))
  (:import java.net.Socket))

(defn create
  "Given a host and a port, generate an IRC connection map, containing
   pairs for the socket, the reader, and the writer."
  [host port]
  (let [socket (Socket. host (Integer. port))]
    {:socket socket
     :in (io/reader socket)
     :out (io/writer socket)}))

(defn print-input [irc]
  (while true
    (binding [*in* (:in irc)]
      (println (read-line)))))

(defn send-message [irc & strings]
  (let [message (string/join " " strings)]
    (binding [*out* (:out irc)]
      (println message))))

(defn register [irc nickname & [password]]
  (send-message irc "NICK" nickname)
  (send-message irc "USER" nickname " i * " nickname)
  (when password
    (send-message irc "PRIVMSG nickserv :identify" nickname password)))

(defn join
  "Join a specified channel."
  [irc channel]
  (send-message irc "JOIN" channel))
