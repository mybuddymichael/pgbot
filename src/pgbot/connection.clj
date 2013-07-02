(ns pgbot.connection
  (:require (pgbot [messages :refer [parse compose]]
                   events)
            [clojure.core.async :as async]))

(defn- register
  "Sends a 'handshake' message to register the connection."
  [connection]
  (let [nick (connection :nick)]
    (pgbot.events/trigger
      connection
      :outgoing
      {:type "NICK" :destination nick}
      {:type "USER" :destination (str nick " i * " nick)})))

(defn- get-line
  "Grabs a single line from the connection, parsing it into a message
   map, or returning nil if the socket is closed."
  [connection]
  (try (->> (connection :in) .readLine parse)
    (catch java.io.IOException _ nil)))

(defn- send-message
  "Sends one or more messages through a connection's writer."
  [connection & messages]
  (binding [*out* (connection :out)]
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
  "Generates a map containing information about the IRC connection."
  [host port nick channel]
  (-> {:host host
       :port port
       :socket nil
       :in nil
       :out nil
       :nick nick
       :channel channel
       :events {}
       :line-loop nil}
      (pgbot.events/register [:incoming] [ping-pong])
      (pgbot.events/register [:outgoing] [send-message])))

(defn start
  "Takes a connection and runs side effects to open it. If it cannot
   establish a connection it will continue trying until it succeeds."
  [{:keys [host port channel] :as connection}]
  (let [open-socket
        (fn [host port]
          (or (try (java.net.Socket. host (Integer. port))
                   (catch java.net.UnknownHostException _ nil))
              (recur host port)))
        socket (open-socket host port)
        connection (assoc connection
                          :socket socket
                          :in (clojure.java.io/reader socket)
                          :out (clojure.java.io/writer socket)) ]
    (register connection)
    (pgbot.events/trigger connection
                          :outgoing
                          {:type "JOIN" :destination channel})
    (assoc connection
           :line-loop
           (future
             (loop [line (get-line connection)]
               (when line
                 (pgbot.events/trigger connection :incoming line)
                 (recur (get-line connection))))))))

(defn stop [{:keys [socket] :as connection}]
  (.close socket)
  connection)
