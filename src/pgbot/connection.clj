(ns pgbot.connection
  (:require (pgbot [messages :refer [parse compose Message]])
            [clojure.core.typed :as t
             :refer [ann def-alias doseq> fn> loop> Nilable Seq typed-deps]]
            [clojure.core.typed.async :refer [Chan chan>]]
            [clojure.core.async :refer [chan thread put! alts!! close!]]))

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
                    :in-chans (Seq (Chan Message))
                    :out-chans (Seq (Chan Message))
                    :kill (Chan Any)}))

(ann get-line [java.io.BufferedReader -> (Nilable Message)])
(defn ^:private get-line
  "Grabs a single line from the connection, parsing it into a message
   map, or returning nil if the socket is closed."
  [reader]
  (try (let [line (.readLine ^java.io.BufferedReader reader)]
         (if line (parse line) nil))
    (catch java.io.IOException _ nil)))

(ann send-message! [java.io.BufferedWriter Message * -> nil])
(defn ^:private send-message!
  "Sends one or more messages through a connection's writer."
  [writer & messages]
  (binding [*out* writer]
    (doseq> [message :- Message messages]
      (println (compose message)))))

(ann create [String Integer String String (Seq (Chan Message))
             (Seq (Chan Message)) -> Connection])
(defn create
  "Creates and returns a map for holding the physical connection to the
   IRC server."
  [host port nick channel in-chans out-chans]
  {:socket nil
   :reader nil
   :writer nil
   :host host
   :port port
   :nick nick
   :channel channel
   :in-chans in-chans
   :out-chans out-chans
   :kill (chan)})

(ann start [Connection -> Connection])
(defn start
  "Runs side effects to open a connection to an IRC server. If it cannot
   establish a connection it will keep trying until it succeeds. It
   starts placing incoming messages into the in channel and taking
   outgoing message off the out channel."
  [{:keys [reader writer host port nick channel in-chans out-chans stop]
    :as connection}]
  (let [open-socket
        (fn> [host :- String
              port :- Integer]
          (or (try (java.net.Socket. ^String host ^Long port)
                   (catch java.net.UnknownHostException _ nil))
              (recur host port)))
        socket (open-socket host port)
        _ (.setSoTimeout ^java.net.Socket socket 300000)
        connection (assoc connection
                          :socket socket
                          :reader (clojure.java.io/reader socket)
                          :writer (clojure.java.io/writer socket))]
    (send-message! (:writer connection)
                   {:type "NICK" :destination nick}
                   {:type "USER" :destination (str nick " i * " nick)})
    (send-message! (:writer connection)
                   {:type "JOIN" :destination channel})
    (thread
      (loop> [line :- (Nilable Message)
              (get-line (:reader connection))]
        (when line
          (doseq> [c :- (Chan Message) in-chans]
            (put! c line))
          (recur (get-line (:reader connection))))))
    (thread
      (t/tc-ignore
        (let [alts-fn #(alts!! (flatten [stop out-chans]) :priority true)]
          (loop [[message chan] (alts-fn)]
            (when (not= chan stop)
              (send-message! writer message)
              (recur (alts-fn)))))))
    connection))

(ann stop [Connection -> Connection])
(defn stop [{:keys [socket reader writer kill] :as connection}]
  (when (and socket reader writer)
    (.close ^java.net.Socket socket)
    (.close ^java.io.BufferedReader reader)
    (.close ^java.io.BufferedWriter writer))
  (close! kill)
  connection)
