(ns pgbot.io
  "Functions for handling I/O.")

(defn send-message
  "Send a message through a connection's writer."
  [connection message & messages]
  (binding [*out* (connection :out)]
    (if messages
      (println (str message " " (clojure.string/join " " messages)))
      (println message))))
