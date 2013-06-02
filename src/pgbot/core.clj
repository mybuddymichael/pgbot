(ns pgbot.core
  "A simple IRC bot."
  (:require [pgbot.connection :as connection]
            [pgbot.messages :use [parse compose]]))

(def ^:private plugins
  #{'pgbot.plugin.help})

(doseq [p plugins]
  (require `~p))

(declare trigger-event)

(defn- print-messages
  [_ & messages]
  (doseq [m messages]
    (println (compose m))))

(defn- log
  "Log a string to a preferred output."
  [_ & messages]
  (doseq [m messages]
    (spit "/tmp/pgbot.log"
          (str (java.util.Date.) " : " (compose m) "\n")
          :append true)))

(defn- register-connection
  "Sends a 'handshake' message to register the connection."
  [connection]
  (let [nick (connection :nick)]
    (trigger-event connection
                   :outgoing
                   (parse (str "NICK " nick))
                   (parse (str "USER " nick " i * " nick)))))

(defn- ping-pong
  "Triggers an outgoing event with a PONG string if the incoming message
   is a PING."
  [connection & messages]
  (doseq [m messages]
    (when (= (m :type) "PING")
      (trigger-event connection :outgoing {:type "PONG" :content (m :content)}))))

(def ^:private events
  "Returns an agent containing a map of event keywords to sets of action
   functions."
  {:incoming #{log
               print-messages
               connection/ping-pong}
   :outgoing #{log
               print-line
               connection/send-message}})

(defn- trigger-event
  "Triggers the specified event, passing in the connection map and data
   to the event's action functions."
  [connection event & data]
  (doseq [f (events event)]
    (apply f connection (flatten data))))

(defn connect
  "Entry point for operating the bot. This creates a connection, does
   the dance to ensure that it stays open, and begins listening for
   messages in a new thread. It returns the connection map."
  [host port nick channel]
  (let [connection (connection/create host port nick channel events)]
    (future
      (register-connection connection)
      (trigger-event connection :outgoing {:type "JOIN" :destination channel})
      (loop [line (connection/get-line connection)]
        (when line
          (trigger-event connection :incoming line)
          (recur (connection/get-line connection)))))
    connection))
