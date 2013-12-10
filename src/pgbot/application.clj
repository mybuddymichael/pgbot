(ns pgbot.application
  (:require [clojure.core.async :as async]
            [taoensso.timbre :refer [info]]
            (pgbot [lifecycle :as lifecycle :refer [Lifecycle]]
                   [commit-server :as commit-server]
                   [connection :as connection]
                   [responder :as responder])))

(def config
  {:buffer-size 20})

(defn update
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
        commit-server (commit-server/create
                        (:buffer-size config) commit-server-port channel)
        responder (responder/create (:buffer-size config))
        connection (connection/create
                     (:buffer-size config) host port nick channel)
        in-chans [(:in responder)]
        out-chans [(:out responder) (:out commit-server)]
        incoming-mult (async/mult (:in connection))
        outgoing-mix (async/mix (:out connection))]
    (doseq [in in-chans] (async/tap incoming-mult in))
    (doseq [out out-chans] (async/admix outgoing-mix out))
    {:connection connection
     :responder responder
     :commit-server commit-server}))

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
