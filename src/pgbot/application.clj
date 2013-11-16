(ns pgbot.application
  (:require (pgbot [lifecycle :as lifecycle :refer [Lifecycle]]
                   [connection :refer [Connection]]
                   commit-server
                   responder)
            [clojure.core.typed :as t :refer [ann def-alias Vec]])
  (:import (clojure.lang Keyword)
           pgbot.responder.Responder
           pgbot.commit_server.CommitServer))

(def-alias Application
  (HMap :mandatory {:connection Connection
                    :responder Responder
                    :commit-server CommitServer}))

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
        commit-server (pgbot.commit-server/->CommitServer commit-server-port
                                                          channel)
        responder (pgbot.responder/->Responder)
        in-chans [(:in responder)]
        out-chans [(:out responder) (:out commit-server)]
        connection (pgbot.connection/create host port nick channel
                                            in-chans out-chans)]
    {:connection connection
     :responder responder
     :commit-server commit-server}))


(ann ^:no-check start [Application -> Application])
(defn start
  "Runs various side effects to start up pgbot. Returns the started
   application."
  [{:keys [connection responder commit-server] :as application}]
  (assoc application
         :connection (pgbot.connection/start connection)
         :responder (lifecycle/start responder)
         :commit-server (lifecycle/start commit-server)))

(ann ^:no-check stop [Application -> Application])
(defn stop
  "Runs various side effects to shut down pgbot. Returns the stopped
   application."
  [{:keys [connection responder commit-server] :as application}]
  (assoc application
         :connection (pgbot.connection/stop connection)
         :responder (lifecycle/stop responder)
         :commit-server (lifecycle/stop commit-server)))
