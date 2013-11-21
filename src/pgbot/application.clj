(ns pgbot.application
  (:require [clojure.core.async :as async]
            [clojure.core.typed :as t :refer [ann def-alias Nilable Seqable]]
            [clojure.core.typed.async :refer [Chan]]
            (pgbot [lifecycle :as lifecycle :refer [Lifecycle]]
                   [commit-server :as commit-server]
                   [connection :as connection :refer [Connection]]
                   [dispatcher :as dispatcher]
                   [messages :refer [Message]]
                   [responder :as responder]))
  (:import (clojure.lang Keyword)
           pgbot.commit_server.CommitServer
           pgbot.dispatcher.Dispatcher
           pgbot.responder.Responder))

(def-alias Application
  (HMap :mandatory {:connection Connection
                    :responder Responder
                    :commit-server CommitServer
                    :dispatcher Dispatcher}))

(ann create [(HMap :mandatory {:host String
                               :port String
                               :nick String
                               :channel String
                               :commit-server-port String})
             -> Application])
(defn create
  "Creates and returns a new instance of pgbot."
  [{:keys [host port nick channel commit-server-port]}]
  (let [port (Integer/parseInt port)
        commit-server-port (Integer/parseInt commit-server-port)
        commit-server (commit-server/->CommitServer commit-server-port
                                                    channel)
        responder (responder/->Responder)
        in-chans [(:in responder)]
        out-chans [(:out responder) (:out commit-server)]
        connection (connection/create host port nick channel)
        dispatcher (dispatcher/map->Dispatcher {:incoming (:in connection)
                                                :outgoing (:out connection)
                                                :in-chans in-chans
                                                :out-chans out-chans})]
    {:connection connection
     :responder responder
     :commit-server commit-server
     :dispatcher dispatcher}))

(ann ^:no-check start [Application -> Application])
(defn start
  "Runs various side effects to start up pgbot. Returns the started
   application."
  [{:keys [connection responder commit-server dispatcher]
    :as application}]
  (assoc application
         :connection (pgbot.connection/start connection)
         :responder (lifecycle/start responder)
         :commit-server (lifecycle/start commit-server)
         :dispatcher (lifecycle/start dispatcher)))

(ann ^:no-check stop [Application -> Application])
(defn stop
  "Runs various side effects to shut down pgbot. Returns the stopped
   application."
  [{:keys [connection responder commit-server dispatcher] :as application}]
  (assoc application
         :connection (pgbot.connection/stop connection)
         :responder (lifecycle/stop responder)
         :commit-server (lifecycle/stop commit-server)
         :dispatcher (lifecycle/stop dispatcher)))
