(ns pgbot.application
  (:require (pgbot [process :as process]
                   connection
                   commit-server
                   ping-pong
                   logger)
            [clojure.core.typed :as t])
  (:import pgbot.process.PProcess))

(t/def-alias application
  (HMap :mandatory {:connection Any
                    :subsystems (t/Seq PProcess)}))

(t/ann create [(HMap :mandatory {:host String
                                 :port String
                                 :nick String
                                 :channel String
                                 :commit-server-port String})
               -> application])
(defn create
  "Creates and returns a new instance of pgbot."
  [{:keys [host port nick channel commit-server-port]}]
  (let [port (Integer. ^String port)
        commit-server-port (Integer. ^String commit-server-port)
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

(t/tc-ignore
 (defn start
  "Runs various side effects to start up pgbot. Returns the started
   application."
  [{:keys [connection subsystems] :as application}]
  (-> application
      (assoc :connection (pgbot.connection/start connection))
      (update-in [:subsystems] (comp doall (partial map process/start)))))

(defn stop
  "Runs various side effects to shut down pgbot. Returns the stopped
   application."
  [{:keys [connection subsystems] :as application}]
  (-> application
      (assoc :connection (pgbot.connection/stop connection))
      (update-in [:subsystems] (comp doall (partial map process/stop)))))
)
