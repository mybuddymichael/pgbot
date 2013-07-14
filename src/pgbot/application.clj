(ns pgbot.application
  (:require (pgbot [lifecycle :as lifecycle]
                   connection
                   commit-server
                   ping-pong
                   dispatcher
                   logger)
            [clojure.core.async :refer [chan]]))

(defn create
  "Creates and returns a new instance of pgbot."
  [{:keys [host port nick channel commit-server-port]}]
  (let [port (Integer. port)
        commit-server-port (Integer. commit-server-port)
        subsystems [(pgbot.commit-server/->CommitServer commit-server-port
                                                        channel)
                    (pgbot.ping-pong/->PingPong)
                    (pgbot.logger/->Logger)]
        in-chans (->> subsystems (map :in) (filter identity))
        out-chans (->> subsystems (map :out) (filter identity))
        out-listeners (->> subsystems (map :out-listener) (filter identity))
        connection (pgbot.connection/create host port nick channel
                                            in-chans out-chans out-listeners)]
    {:connection connection
     :subsystems subsystems}))

(defn create-dev
  "Creates and returns a new pgbot instance suitable for development."
  [& _]
  (create {:host "irc.freenode.net"
           :port 6667
           :nick "pgbottest"
           :channel "##pgbottest"
           :commit-server-port 8080}))

(defn start
  "Runs various side effects to start up pgbot. Returns the started
   application."
  [{:keys [connection subsystems] :as application}]
  (-> application
      (assoc :connection connection)
      (update-in [:subsystems] (partial map lifecycle/start))))

(defn stop
  "Runs various side effects to shut down pgbot. Returns the stopped
   application."
  [{:keys [connection commit-server ping-pong dispatcher] :as application}]
  (-> application
      (assoc :connection (pgbot.connection/stop connection))
      (update-in [:subsystems] (partial map lifecycle/stop))))
