(ns pgbot.events)

(defn register
  "Takes a list of functions and adds them to the set of events to be
   fired when the specified events are triggered."
  [es fs]
  (doseq [e es f fs]
    (swap! events assoc e (conj (@events e #{}) f))))

(defn trigger
  "Triggers the specified event, passing in the connection map and data
   to the event's action functions."
  [connection event & data]
  (doseq [f (event (:events connection))]
    (apply f connection (flatten data))))
