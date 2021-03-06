(ns pgbot.connection
  (:require [clojure.core.async :as async]
            [taoensso.timbre :refer [debug info error]]
            (pgbot [lifecycle :refer [Lifecycle]]
                   [messages :as messages :refer [parse compose]])))

(defprotocol Closeable
  (close! [this]))

(extend-protocol Closeable
  java.io.BufferedWriter
  (close! [w] (.close w))
  java.io.BufferedReader
  (close! [r] (.close r))
  java.net.Socket
  (close! [s] (.close s))
  clojure.core.async.impl.channels.ManyToManyChannel
  (close! [c] (async/close! c)))

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
                       dead]
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
      (async/thread
        (loop [messages (message-seq reader)]
          (if-let [message (first messages)]
            (do
              (info "Read message" (hash message))
              (debug "Message" (hash message) "is" message)
              (async/>!! in message)
              (info "Message" (hash message) "placed on in.")
              (when (= (:type message) "ERROR")
                (error "ERROR message received:" message))
              (recur (rest messages)))
            (.stop connection))))
      (async/thread
        (loop [message (async/<!! out)]
          (when message
            (send-message! writer message)
            (info "Message" (hash message) "sent to the writer.")
            (recur (async/<!! out)))))
      (info "Connection started.")
      (assoc connection
             :socket socket
             :reader reader
             :writer writer)))
  (stop [connection]
    (doseq [closeable [socket reader writer in out dead]]
      (close! closeable))
    (info "Connection stopped.")
    connection))

(defn create
  "Creates and returns a map for holding the physical connection to the
   IRC server."
  [buffer-size host port nick channel]
  (map->Connection {:socket nil
                    :reader nil
                    :writer nil
                    :host host
                    :port port
                    :nick nick
                    :channel channel
                    :in (async/chan buffer-size)
                    :out (async/chan buffer-size)
                    :dead (async/chan)}))
