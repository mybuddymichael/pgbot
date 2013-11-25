(ns pgbot.connection
  (:require [clojure.core.typed :as t
             :refer [ann ann-record def-alias doseq> fn> loop> Nilable Seq
                     typed-deps]]
            [clojure.core.typed.async :refer [Chan chan>]]
            [clojure.core.async :refer [<!! chan thread put! close!]]
            [taoensso.timbre :refer [info]]
            (pgbot annotations
                   [lifecycle :refer [Lifecycle]]
                   [messages :as messages :refer [parse compose Message]])))

(typed-deps clojure.core.typed.async)

(ann message-seq [java.io.BufferedReader -> (Nilable (Seq Message))])
(defn message-seq
  "Like line-seq, but it catches IOExceptions from the socket,
   in which case it will return nil. Lines are parsed into Message maps."
  [^java.io.BufferedReader reader]
  (when-let [line (try (.readLine reader)
                    (catch java.io.IOException _ nil))]
    (cons (parse line) (lazy-seq (message-seq reader)))))

(ann send-message! [java.io.BufferedWriter Message * -> nil])
(defn send-message!
  "Sends one or more messages through a connection's writer."
  [writer & messages]
  (binding [*out* writer]
    (doseq> [message :- Message messages]
      (t/tc-ignore
        (info "Sending outgoing message" (:uuid message) "to the writer."))
      (println (compose message)))))

(ann-record Connection [socket :- (Nilable java.net.Socket)
                        reader :- (Nilable java.io.BufferedReader)
                        writer :- (Nilable java.io.BufferedWriter)
                        host :- String
                        port :- Integer
                        nick :- String
                        channel :- String
                        in :- (Chan Message)
                        out :- (Chan Message)
                        kill :- (Chan Nothing)])
(defrecord Connection [socket
                       reader
                       writer
                       host
                       port
                       nick
                       channel
                       in
                       out
                       kill]
  Lifecycle
  (start [connection]
    (let [open-socket
          (fn> [host :- String
                port :- Integer]
               (or (try (java.net.Socket. ^String host ^Long port)
                     (catch java.io.IOException  _ nil))
                   (recur host port)))
          socket (open-socket host port)
          _ (.setSoTimeout ^java.net.Socket socket 300000)
          connection (assoc connection
                            :socket socket
                            :reader (clojure.java.io/reader socket)
                            :writer (clojure.java.io/writer socket))]
      (send-message! (:writer connection)
                     (messages/parse (str "NICK " nick))
                     (messages/parse (str "USER " nick " i * " nick)))
      (send-message! (:writer connection)
                     (messages/parse (str "JOIN " channel)))
      (thread
        (loop> [messages :- (Nilable (Seq Message))
                (message-seq (:reader connection))]
          (if-let [message (first messages)]
            (do (put! in message)
              (t/tc-ignore (info "Incoming message" (:uuid message) "placed on in."))
              (recur (rest messages)))
            (close! kill))))
      (thread
        (loop> [message :- (Nilable Message)
                (<!! out)]
          (when message
            (send-message! (:writer connection) message)
            (recur (<!! out)))))
      (t/tc-ignore (info "Connection started."))
      connection))
  (stop [connection]
    (when (and socket reader writer)
      (.close ^java.net.Socket socket)
      (.close ^java.io.BufferedReader reader)
      (.close ^java.io.BufferedWriter writer))
    (close! kill)
    connection))

(ann create [String Integer String String -> Connection])
(defn create
  "Creates and returns a map for holding the physical connection to the
   IRC server."
  [host port nick channel]
  (map->Connection {:socket nil
                    :reader nil
                    :writer nil
                    :host host
                    :port port
                    :nick nick
                    :channel channel
                    :in (chan> Message)
                    :out (chan> Message)
                    :kill (chan> Nothing)}))
