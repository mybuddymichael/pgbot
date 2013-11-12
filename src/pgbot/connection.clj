(ns pgbot.connection
  (:require (pgbot [messages :refer [parse compose Message]])
            [clojure.core.typed :as t]
            [clojure.core.typed.async :refer [Chan chan>]]
            [clojure.core.async :refer [chan thread put! alts!! close!]]))

(t/typed-deps clojure.core.typed.async)

(t/ann ^:no-check clojure.java.io/reader [Any -> java.io.BufferedReader])
(t/ann ^:no-check clojure.java.io/writer [Any -> java.io.BufferedWriter])

(t/def-alias Connection
  (HMap :mandatory {:socket (t/Nilable java.net.Socket)
                    :reader (t/Nilable java.io.BufferedReader)
                    :writer (t/Nilable java.io.BufferedWriter)
                    :host String
                    :port Integer
                    :nick String
                    :channel String
                    :in-loop (t/Nilable (Chan Any))
                    :out-loop (t/Nilable (Chan Any))
                    :in-chans (t/Seq (Chan Message))
                    :out-chans (t/Seq (Chan Message))
                    :out-listeners (t/Seq (Chan Message))
                    :kill (Chan Any)}))

(t/ann get-line [java.io.BufferedReader -> (t/Nilable Message)])
(defn- get-line
  "Grabs a single line from the connection, parsing it into a message
   map, or returning nil if the socket is closed."
  [reader]
  (try (let [line (.readLine ^java.io.BufferedReader reader)]
         (if line (parse line) nil))
    (catch java.io.IOException _ nil)))

(t/ann send-message! [java.io.BufferedWriter Message * -> nil])
(defn- send-message!
  "Sends one or more messages through a connection's writer."
  [writer & messages]
  (binding [*out* writer]
    (t/doseq> [message :- Message messages]
      (println (compose message)))))

(t/ann create [String Integer String String (t/Seq (Chan Message))
               (t/Seq (Chan Message)) -> Connection])
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
   :in-loop nil
   :out-loop nil
   :in-chans in-chans
   :out-chans out-chans
   :kill (chan)})

(t/ann ^:no-check start [Connection -> Connection])
(defn start
  "Runs side effects to open a connection to an IRC server. If it cannot
   establish a connection it will keep trying until it succeeds. It
   starts placing incoming messages into the in channel and taking
   outgoing message off the out channel."
  [{:keys [reader writer host port nick channel in-chans out-chans stop]
    :as connection}]
  (let [open-socket
        (t/fn> [host :- String
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
    (assoc connection
           :in-loop
           (thread
             (t/loop> [line :- (t/Nilable Message)
                       (get-line (:reader connection))]
               (when line
                 (t/doseq> [c :- (Chan Message) in-chans]
                   (put! c line))
                 (recur (get-line (:reader connection))))))
           :out-loop
           (thread
             (t/tc-ignore
             (let [alts-fn #(alts!! (flatten [stop out-chans]) :priority true)]
               (loop [[message chan] (alts-fn)]
                 (when (not= chan stop)
                   (send-message! writer message)
                   (recur (alts-fn))))))))))

(t/ann stop [Connection -> Connection])
(defn stop [{:keys [socket reader writer kill] :as connection}]
  (when (and socket reader writer)
    (.close ^java.net.Socket socket)
    (.close ^java.io.BufferedReader reader)
    (.close ^java.io.BufferedWriter writer))
  (close! kill)
  connection)
