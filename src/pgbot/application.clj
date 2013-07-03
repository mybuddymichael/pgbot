(ns pgbot.application
  (:require (pgbot connection
                   logger
                   commit-server)))

(defn create-dev
  "Creates and returns a new pgbot instance suitable for development."
  (create :host "irc.freenode.net"
          :port 6667
          :nick "pgbottest"
          :channel "##pgbottest"
          :commit-server-port 8080))

(defn create
  "Creates and returns a new instance of pgbot."
  [& {:keys [host port nick channel commit-server-port]
      :or {host "irc.freenode.net"
           port 6667
           nick "pgbottest"
           channel "##pgbottest"
           commit-server-port 8080}}]
  (let [connection (pgbot.connection/create host port nick channel)]
    {:connection connection
     :commit-server (pgbot.commit-server/create commit-server-port
                                                (connection :out)
                                                (connection :channel))}))

(defn start
  "Runs various side effects to start up pgbot. Returns the started
   application."
  [{:keys [connection commit-server] :as application}]
  (let [connection
        (-> connection
            pgbot.logger/start
            pgbot.connection/start)]
    (assoc application
           :connection connection
           :commit-server (pgbot.commit-server/start commit-server))))

(defn stop
  "Runs various side effects to shut down pgbot. Returns the stopped
   application."
  [{:keys [connection commit-server] :as application}]
  (assoc application
         :commit-server (pgbot.commit-server/stop commit-server)
         :connection (pgbot.connection/stop connection)))
