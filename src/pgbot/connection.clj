(ns pgbot.connection
  (:require [pgbot.messages :refer [parse compose]]
            [pgbot.events :refer [trigger-event]]))

(defn create
  "Generates a map containing information about the IRC connection."
  [host port nick channel]
  {:host host
   :post port
   :in nil
   :socket nil
   :out nil
   :nick nick
   :channel channel})

(defn register
  "Sends a 'handshake' message to register the connection."
  [connection]
  (let [nick (connection :nick)]
    (trigger-event connection
                   :outgoing
                   (parse (str "NICK " nick))
                   (parse (str "USER " nick " i * " nick)))))

(defn get-line
  "Grabs a single line from the connection, parsing it into a message
   map, or returning nil if the socket is closed."
  [connection]
  (try (->> (connection :in) .readLine parse)
    (catch java.net.SocketException _ nil)))

(defn send-message
  "Sends one or more messages through a connection's writer."
  [connection & messages]
  (binding [*out* (connection :out)]
    (doseq [m messages]
      (println (compose m)))))

(defn ping-pong
  "Triggers an outgoing event with a PONG string if the incoming message
   is a PING."
  [connection & messages]
  (doseq [m messages]
    (when (= (m :type) "PING")
      (trigger-event connection
                     :outgoing
                     {:type "PONG" :content (m :content)}))))
