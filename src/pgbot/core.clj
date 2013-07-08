(ns pgbot.core
  "A simple IRC bot."
  (:gen-class)
  (:require (pgbot system)))

(defn -main
  "Start pgbot. This will block until the connection closes, at which
   point it will automatically try to reconnect."
  [host port nick channel commit-server-port]
  (let [system (-> (pgbot.system/create :host host :port port :nick nick
                                        :channel channel
                                        :commit-server-port commit-server-port)
                   pgbot.system/start)]
    @(get-in system [:connection :line-loop])
    (pgbot.system/stop system)
    (recur host port nick channel commit-server-port)))
