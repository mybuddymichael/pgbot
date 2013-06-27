(ns pgbot.system
  (:require (pgbot connection
                   logger
                   commit-server)))

(defn create
  "Creates and returns a new instance of pgbot."
  [& {:keys [host port nick channel]
      :or {host "irc.freenode.net"
           port 6667
           nick "pgbottest"
           channel "##pgbottest"}}]
  {:connection (pgbot.connection/create host port nick channel)})

(defn start [{:keys [connection] :as system}]
  "Runs various side effects to start up pgbot. Returns the started
   application."
  (let [connection
        (-> connection
            pgbot.logger/start
            pgbot.connection/start)]
    (assoc system
           :connection connection
           :commit-server (pgbot.commit-server/create-and-start connection))))

(defn stop [{:keys [connection commit-server] :as system}]
  "Runs various side effects to shut down pgbot. Returns the stopped
   application."
  (assoc system
         :commit-server (pgbot.commit-server/stop commit-server)
         :connection (pgbot.connection/stop connection)))
