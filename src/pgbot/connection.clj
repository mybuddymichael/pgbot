(ns pgbot.connection
  (:require (pgbot [messages :refer [parse compose]]
                   events)
            [clojure.core.async :refer [chan go >! <!]]))

(defn- get-line
  "Grabs a single line from the connection, parsing it into a message
   map, or returning nil if the socket is closed."
  [connection]
  (try (->> (connection :reader) .readLine parse)
    (catch java.io.IOException _ nil)))

(defn- send-message
  "Sends one or more messages through a connection's writer."
  [connection & messages]
  (binding [*out* (connection :writer)]
    (doseq [m messages]
      (println (compose m)))))

(defn- ping-pong
  "Triggers an outgoing event with a PONG string if the incoming message
   is a PING."
  [connection & messages]
  (doseq [m messages]
    (when (= (m :type) "PING")
      (pgbot.events/trigger
        connection
        :outgoing
        {:type "PONG" :content (m :content)}))))

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
   :in (chan)
   :out (chan)})

(defn start
  "Takes a connection and runs side effects to open it. If it cannot
   establish a connection it will continue trying until it succeeds."
  [{:keys [host port nick channel in out] :as connection}]
  (let [open-socket
        (fn [host port]
          (or (try (java.net.Socket. host port)
                   (catch java.net.UnknownHostException _ nil))
              (recur host port)))
        socket (open-socket host port)
        connection (assoc connection
                          :socket socket
                          :reader (clojure.java.io/reader socket)
                          :writer (clojure.java.io/writer socket))]
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

(defn stop [{:keys [socket] :as connection}]
  (.close socket)
  connection)
