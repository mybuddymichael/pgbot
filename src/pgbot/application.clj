(ns pgbot.application
  (:require (pgbot [lifecycle :as lifecycle :refer [Lifecycle]]
                   [connection :refer [Connection]]
                   commit-server
                   responder)
            [clojure.core.typed :as t :refer [ann def-alias Vec]])
  (:import (clojure.lang Keyword)))

(def-alias Application
  (HMap :mandatory {:connection Connection
                    :subsystems (t/Vec Lifecycle)}))

(ann create [(HMap :mandatory {:host String
                                 :port String
                                 :nick String
                                 :channel String
                                 :commit-server-port String})
             -> Application])
(defn create
  "Creates and returns a new instance of pgbot."
  [{:keys [host port nick channel commit-server-port]}]
  (let [port (Integer. ^String port)
        commit-server-port (Integer. ^String commit-server-port)
        subsystems [(pgbot.commit-server/->CommitServer commit-server-port
                                                        channel)
                    (pgbot.responder/->Responder)]
        in-chans (->> subsystems (map :in) (filter identity))
        out-chans (->> subsystems (map :out) (filter identity))
        connection (pgbot.connection/create host port nick channel
                                            in-chans out-chans)]
    {:connection connection
     :subsystems subsystems}))


(ann start [Application -> Application])
(defn start
  "Runs various side effects to start up pgbot. Returns the started
   application."
  [{:keys [connection subsystems] :as application}]
  (-> application
      (assoc :connection (pgbot.connection/start connection))
      (update-in [:subsystems] (comp doall (partial map lifecycle/start)))))

(ann stop [Application -> Application])
(defn stop
  "Runs various side effects to shut down pgbot. Returns the stopped
   application."
  [{:keys [connection subsystems] :as application}]
  (-> application
      (assoc :connection (pgbot.connection/stop connection))
      (update-in [:subsystems] (comp doall (partial map lifecycle/stop)))))
