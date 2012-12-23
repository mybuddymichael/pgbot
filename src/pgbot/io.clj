(ns pgbot.io
  "Functions for handling I/O.")

(defn send-message
  "Send a message through a connection's writer. This takes multiple
  string arguments and will join them with spaces in between."
  [connection message & messages]
  (binding [*out* (connection :out)]
    (println (clojure.string/join " " (cons message messages)))))
