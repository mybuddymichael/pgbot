(ns pgbot.core
  "A simple IRC bot."
  (:gen-class)
  (:require (pgbot application)
            [clojure.core.async :refer [<!!]]))

(defn -main
  "Start pgbot. This will block until the connection closes, at which
   point it will automatically try to reconnect."
  [host port nick channel commit-server-port]
  (let [application (-> (pgbot.application/create
                          {:host host :port port :nick nick :channel channel
                           :commit-server-port commit-server-port})
                        pgbot.application/start)]
    (<!! (get-in application [:connection :in-loop]))
    (pgbot.application/stop application)
    (recur host port nick channel commit-server-port)))
