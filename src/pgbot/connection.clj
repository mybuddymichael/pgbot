(ns pgbot.connection
  (:require (pgbot [messages :as messages :refer [parse compose Message]])
            [clojure.core.typed :as t
             :refer [ann def-alias doseq> fn> loop> Nilable Seq typed-deps]]
            [clojure.core.typed.async :refer [Chan chan>]]
            [clojure.core.async :refer [<!! chan thread put! close!]]
            [taoensso.timbre :refer [info]]))

(typed-deps clojure.core.typed.async)

(ann ^:no-check clojure.java.io/reader [Any -> java.io.BufferedReader])
(ann ^:no-check clojure.java.io/writer [Any -> java.io.BufferedWriter])

(ann ^:no-check clojure.core.async/put!
  (All [a] (Fn [(Chan a) a -> nil]
               [(Chan a) a [Any * -> Any] -> nil]
               [(Chan a) a [Any * -> Any] Boolean -> nil])))

(def-alias Connection
  (HMap :mandatory {:socket (Nilable java.net.Socket)
                    :reader (Nilable java.io.BufferedReader)
                    :writer (Nilable java.io.BufferedWriter)
                    :host String
                    :port Integer
                    :nick String
                    :channel String
                    :in (Chan Message)
                    :out (Chan Message)
                    :kill (Chan Nothing)}))

(ann message-seq [java.io.BufferedReader -> (Nilable (Seq Message))])
(defn ^:private message-seq
  "Like line-seq, but it catches IOExceptions from the socket,
   in which case it will return nil. Lines are parsed into Message maps."
  [^java.io.BufferedReader reader]
  (when-let [line (try (.readLine reader)
                    (catch java.io.IOException _ nil))]
    (cons (parse line) (lazy-seq (message-seq reader)))))

(ann send-message! [java.io.BufferedWriter Message * -> nil])
(defn ^:private send-message!
  "Sends one or more messages through a connection's writer."
  [writer & messages]
  (binding [*out* writer]
    (doseq> [message :- Message messages]
      (t/tc-ignore
        (info "Sending outgoing message" (:uuid message) "to the writer."))
      (println (compose message)))))

(ann create [String Integer String String -> Connection])
(defn create
  "Creates and returns a map for holding the physical connection to the
   IRC server."
  [host port nick channel]
  {:socket nil
   :reader nil
   :writer nil
   :host host
   :port port
   :nick nick
   :channel channel
   :in (chan> Message)
   :out (chan> Message)
   :kill (chan> Nothing)})

(ann start [Connection -> Connection])
(defn start
  "Runs side effects to open a connection to an IRC server. If it cannot
   establish a connection it will keep trying until it succeeds. It
   starts placing incoming messages into the in channel and taking
   outgoing message off the out channel."
  [{:keys [reader writer host port nick channel in out stop]
    :as connection}]
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
              (t/tc-ignore (info "Incoming message" (:uuid message) "put! on in."))
              (recur (rest messages)))
            (close! in))))
    (thread
      (loop> [message :- (Nilable Message)
              (<!! out)]
        (when message
          (send-message! (:writer connection) message)
          (recur (<!! out)))))
    (t/tc-ignore (info "Connection started."))
    connection))

(ann stop [Connection -> Connection])
(defn stop [{:keys [socket reader writer kill] :as connection}]
  (when (and socket reader writer)
    (.close ^java.net.Socket socket)
    (.close ^java.io.BufferedReader reader)
    (.close ^java.io.BufferedWriter writer))
  (close! kill)
  connection)
