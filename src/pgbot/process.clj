(ns pgbot.process
  (:require [clojure.core.typed :as t]))

(t/ann-protocol PProcess
                start [PProcess -> PProcess]
                stop [PProcess -> PProcess])
(t/defprotocol> PProcess
  "A protocol for starting and stopping processes."
  (start [system] "Runs side effects to start the process. Returns the process.")
  (stop [system] "Runs side effects to stop the process. Returns the process."))
