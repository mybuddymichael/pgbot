(ns pgbot.events)

(defn trigger
  "Triggers the specified event, passing in the connection map and data
   to the event's action functions."
  [connection event & data]
  (doseq [f (event (:events connection))]
    (apply f connection (flatten data))))
