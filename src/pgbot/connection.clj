(ns pgbot.connection
  (:require (pgbot [messages :refer [parse compose]]
                   events)
            [clojure.core.async :refer [chan go >! <! close!]]))

(defn- get-line
  "Grabs a single line from the connection, parsing it into a message
   map, or returning nil if the socket is closed."
  [connection]
  (try (->> (connection :reader) .readLine parse)
    (catch java.io.IOException _ nil)))

(defn- send-message
  "Sends one or more messages through a connection's writer."
  [connection message]
  (binding [*out* (connection :writer)]
    (println (compose message))))

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
   :in nil
   :out nil
   :in-loop nil
   :out-loop nil})

(defn start
  "Runs side effects to open a connection to an IRC server. If it cannot
   establish a connection it will keep trying until it succeeds. It
   starts placing incoming messages into the in channel and taking
   outgoing message off the out channel."
  [{:keys [host port nick channel in out] :as connection}]
  (let [open-socket
        (fn [host port]
          (or (try (java.net.Socket. host port)
                   (catch java.net.UnknownHostException _ nil))
              (recur host port)))
        socket (open-socket host port)
        in (chan)
        out (chan)
        connection (assoc connection
                          :socket socket
                          :reader (clojure.java.io/reader socket)
                          :writer (clojure.java.io/writer socket)
                          :in in
                          :out out)]
    (send-message connection
                  {:type "NICK" :destination nick}
                  {:type "USER" :destination (str nick " i * " nick)})
    (send-message connection
                  {:type "JOIN" :destination channel})
    (assoc connection
           :in-loop
           (go
             (loop [line (get-line connection)]
               (when line
                 (>! in (parse line))
                 (recur (get-line connection)))))
           :out-loop
           (go
             (loop [message (<! out)]
               (when message
                 (send-message connection message)
                 (recur (<! out))))))))

(defn stop [{:keys [socket out] :as connection}]
  (.close socket)
  (close! out)
  connection)
