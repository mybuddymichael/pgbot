(ns pgbot.application
  (:require [clojure.core.async :as async]
            [datomic.api :as d]
            [taoensso.timbre :refer [info]]
            (pgbot [lifecycle :as lifecycle :refer [Lifecycle]]
                   [commit-server :as commit-server]
                   [connection :as connection]
                   [dispatcher :as dispatcher]
                   [responder :as responder])))

(def db-uri "datomic:mem://pgbot")

(defn ^:private get-db-conn
  "Creates a database for the uri if one doesn't already exist, connects
   and returns the connection."
  [uri]
  (d/create-database uri)
  (d/connect uri))

(defn ^:private update
  "Takes an application map and a lifecycle function applies the
   function to each component. Returns the updated application."
  [application lifecycle-fn]
  (let [components
        (-> (map (fn [[k v]] [k (lifecycle-fn v)]) application)
            flatten)]
    (apply assoc application components)))

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
  [application]
  (info "Starting subsystems.")
  (update application lifecycle/start))

(defn stop
  "Runs various side effects to shut down pgbot. Returns the stopped
   application."
  [application]
  (info "Stopping subsystems.")
  (update application lifecycle/stop))
