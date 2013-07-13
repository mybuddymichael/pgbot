(ns pgbot.lifecycle
  "A protocol for starting and stopping systems.")

(defprotocol Lifecycle
  (start [system] "Runs side effects to start the system.")
  (stop [system] "Runs side effects to stop the sytem."))
