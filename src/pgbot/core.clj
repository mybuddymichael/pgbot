(ns pgbot.core
  "A simple IRC bot."
  (:gen-class)
  (:require [pgbot.connection :as connection]
            [pgbot.messages :use [parse compose]]
            [pgbot.events :use [trigger-event]]
            [pgbot.git-listener :as git-listener]
            [pgbot.output :as output]))

(def ^:private events
  "Returns an agent containing a map of event keywords to sets of action
   functions."
  {:incoming #{output/log
               output/print-messages
               connection/ping-pong}
   :outgoing #{output/log
               output/print-messages
               connection/send-message}})

(defn connect
  "Entry point for operating the bot. This creates a connection, does
   the dance to ensure that it stays open, and begins listening for
   messages in a new thread. It returns the connection map."
  [host port nick channel]
  (let [connection (connection/create host port nick channel events)]
    (future
      (connection/register connection)
      (trigger-event connection :outgoing {:type "JOIN" :destination channel})
      (loop [line (connection/get-line connection)]
        (when line
          (trigger-event connection :incoming line)
          (recur (connection/get-line connection)))))
    connection))

(defn -main [host port nick channel git-listener-port]
  (let [connection (connect host port nick channel)]
    (git-listener/start-server connection :port git-listener-port)))
