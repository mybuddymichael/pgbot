(ns pgbot.lifecycle)

(defprotocol Lifecycle
  "A protocol for starting and stopping systems."
  (start [system] "Runs side effects to start the system. Returns the system.")
  (stop [system] "Runs side effects to stop the sytem. Returns the system."))
