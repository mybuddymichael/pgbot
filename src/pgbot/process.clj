(ns pgbot.process
  (:require [clojure.core.typed :as t]))

(t/ann-protocol Process
                start [Process -> Process]
                start [Process -> Process])
(t/defprotocol> Process
  "A protocol for starting and stopping processes."
  (start [system] "Runs side effects to start the process. Returns the process.")
  (stop [system] "Runs side effects to stop the process. Returns the process."))
