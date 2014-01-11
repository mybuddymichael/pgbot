(ns pgbot.application
  (:require [clojure.core.async :as async]
            [datomic.api :as d]
            [taoensso.timbre :refer [info]]
            (pgbot [lifecycle :as lifecycle :refer [Lifecycle]]
                   [commit-server :as commit-server]
                   [connection :as connection]
                   [recorder :as recorder]
                   [responder :as responder])))

(def config
  {:buffer-size 100
   :db-uri "datomic:mem://pgbot"})

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
  (let [updated-components
        (->> (map (fn [[k v]] [k (lifecycle-fn v)]) (:components application))
             flatten
             (apply hash-map))]
    (assoc-in application [:components] updated-components)))

(defn create
  "Creates and returns a new instance of pgbot."
  [{:keys [host port nick channel commit-server-port]}]
  (let [port (Integer/parseInt port)
        commit-server-port (Integer/parseInt commit-server-port)
        db-conn (get-db-conn (:db-uri config))
        recorder (recorder/create (:buffer-size config) db-conn)
        commit-server (commit-server/create
                        (:buffer-size config) commit-server-port channel)
        responder (responder/create (:buffer-size config))
        conn (connection/create (:buffer-size config) host port nick channel)
        in-chans [(:in responder) (:in recorder)]
        out-chans [(:out responder) (:out commit-server)]
        incoming-mult (async/mult (:in conn))
        outgoing-mix (async/mix (:out conn))]
    (d/transact db-conn (read-string (slurp "resources/pgbot/schema.edn")))
    (doseq [in in-chans] (async/tap incoming-mult in))
    (doseq [out out-chans] (async/admix outgoing-mix out))
    {:components {:connection conn
                  :responder responder
                  :commit-server commit-server
                  :recorder recorder}
     :db-conn db-conn}))

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
