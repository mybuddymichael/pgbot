(ns pgbot.io
  "Functions for handling I/O.")

(defn send-message
  "Send a message through a connection's writer."
  [connection message]
  (binding [*out* (connection :out)]
    (println message)))
