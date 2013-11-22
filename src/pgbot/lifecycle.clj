(ns pgbot.lifecycle
  (:require [clojure.core.typed :as t]))

(t/ann-protocol Lifecycle
                start [Lifecycle -> Lifecycle]
                stop [Lifecycle -> Lifecycle])
(t/defprotocol> Lifecycle
  "A protocol for starting and stopping systems."
  (start [system] "Runs side effects to start the system. Returns the system.")
  (stop [system] "Runs side effects to stop the system. Returns the system."))
