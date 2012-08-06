(ns pgbot.core
  (:require [pgbot.connection :as connection]))

(defn -main [host port nickname & [password channel]]
  (while true
    (let [irc (connection/create host port)]
      (if password
        (connection/register irc nickname password)
        (connection/register irc nickname))
      (when channel
        (connection/send-message irc (str "JOIN #" channel)))
      (connection/print-input irc))))
