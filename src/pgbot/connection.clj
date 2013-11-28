(ns pgbot.connection
  (:require [clojure.core.async :refer [<!! chan thread put! close!]]
            [taoensso.timbre :refer [info]]
            (pgbot [lifecycle :refer [Lifecycle]]
                   [messages :as messages :refer [parse compose]])))

(defn message-seq
  "Like line-seq, but it catches IOExceptions from the socket,
   in which case it will return nil. Lines are parsed into Message maps."
  [reader]
  (when-let [line (try (.readLine reader)
                    (catch java.io.IOException _))]
    (cons (parse line) (lazy-seq (message-seq reader)))))

(defn send-message!
  "Sends one or more messages through a connection's writer."
  [writer & messages]
  (binding [*out* writer]
    (doseq [message messages]
      (info "Sending outgoing message" (:uuid message) "to the writer.")
      (println (compose message)))))

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
          (fn [host port]
            (or (try (java.net.Socket. host port)
                  (catch java.io.IOException  _ nil))
                (recur host port)))
          socket (open-socket host port)
          _ (.setSoTimeout socket 300000)
          reader (clojure.java.io/reader socket)
          writer (clojure.java.io/writer socket)]
      (send-message! writer
                     (messages/parse (str "NICK " nick))
                     (messages/parse (str "USER " nick " i * " nick)))
      (send-message! writer
                     (messages/parse (str "JOIN " channel)))
      (thread
        (loop [messages (message-seq reader)]
          (if-let [message (first messages)]
            (do
              (put! in message)
              (info "Incoming message" (:uuid message) "placed on in.")
              (recur (rest messages)))
            (close! kill))))
      (thread
        (loop [message (<!! out)]
          (when message
            (send-message! writer message)
            (recur (<!! out)))))
      (info "Connection started.")
      (assoc connection
             :socket socket
             :reader reader
             :writer writer)))
  (stop [connection]
    (doseq [closeable [socket reader writer]]
      (.close closeable))
    (close! kill)
    connection))

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
                    :in (chan)
                    :out (chan)
                    :kill (chan)}))
