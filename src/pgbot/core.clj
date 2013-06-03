(ns pgbot.core
  "A simple IRC bot."
  (:gen-class)
  (:require [pgbot.connection :as connection]
            [pgbot.messages :use [parse compose]]
            [pgbot.events :use [trigger-event]]
            [pgbot.git-listener :as git-listener]))

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

(def ^:private events
  "Returns an agent containing a map of event keywords to sets of action
   functions."
  {:incoming #{log
               print-messages
               connection/ping-pong}
   :outgoing #{log
               print-messages
               connection/send-message}})

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

(defn -main [host port nick channel]
  (let [connection (connect host port nick channel)]
    (reset! git-listener/connection connection)
    (git-listener/start-server)))
