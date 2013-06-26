(ns pgbot.system
  (:require (pgbot connection
                   logger)))

(defn create
  "Creates and returns a new instance of pgbot."
  [& {:keys [host port nick channel]
      :or {host "irc.freenode.net"
           port 6667
           nick "pgbottest"
           channel "##pgbottest"}}]
  {:connection (pgbot.connection/create host port nick channel)})

(defn start [system]
  "Runs various side effects to start up pgbot. Returns the started
   application."
  (->> (system :connection)
       pgbot.logger/start
       pgbot.connection/start
       (assoc system :connection)))

(defn stop [system]
  "Runs various side effects to shut down pgbot. Returns the stopped
   application."
  (pgbot.connection/stop (system :connection))
  system)
