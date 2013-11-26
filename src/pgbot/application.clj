(ns pgbot.application
  (:require [clojure.core.async :as async]
            [taoensso.timbre :refer [info]]
            (pgbot [lifecycle :as lifecycle :refer [Lifecycle]]
                   [commit-server :as commit-server]
                   [connection :as connection]
                   [dispatcher :as dispatcher]
                   [responder :as responder])))

(defn create
  "Creates and returns a new instance of pgbot."
  [{:keys [host port nick channel commit-server-port]}]
  (let [port (Integer/parseInt port)
        commit-server-port (Integer/parseInt commit-server-port)
        commit-server (commit-server/create commit-server-port channel)
        responder (responder/create)
        in-chans [(:in responder)]
        out-chans [(:out responder) (:out commit-server)]
        connection (connection/create host port nick channel)
        dispatcher (dispatcher/create {:incoming (:in connection)
                                       :outgoing (:out connection)
                                       :in-chans in-chans
                                       :out-chans out-chans})]
    {:connection connection
     :responder responder
     :commit-server commit-server
     :dispatcher dispatcher}))

(defn start
  "Runs various side effects to start up pgbot. Returns the started
   application."
  [{:keys [connection responder commit-server dispatcher] :as application}]
  (info "Starting subsystems.")
  (assoc application
         :connection (lifecycle/start connection)
         :responder (lifecycle/start responder)
         :commit-server (lifecycle/start commit-server)
         :dispatcher (lifecycle/start dispatcher)))

(defn stop
  "Runs various side effects to shut down pgbot. Returns the stopped
   application."
  [{:keys [connection responder commit-server dispatcher] :as application}]
  (info "Stopping subsystems.")
  (assoc application
         :connection (lifecycle/stop connection)
         :responder (lifecycle/stop responder)
         :commit-server (lifecycle/stop commit-server)
         :dispatcher (lifecycle/stop dispatcher)))
