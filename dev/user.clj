(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [clojure.repl :refer :all]
            [clojure.pprint :refer [pprint]]
            [clojure.test :refer [run-tests]]
            [taoensso.timbre :as timbre]
            [datomic.api :as d]
            (pgbot application debug)))

(def application nil)

(defn create
  "Creates and stores a new application instance."
  []
  (alter-var-root #'application (constantly (pgbot.application/create
                                              {:host "irc.freenode.net"
                                               :port "6667"
                                               :nick "pgbottest"
                                               :channel "##pgbottest"
                                               :web-server-port "8080"}))))

(defn start
  "Starts the current application."
  []
  (alter-var-root #'application pgbot.application/start))

(defn stop
  "Shuts down the application and releases resources."
  []
  (alter-var-root #'application (fn [s] (when s (pgbot.application/stop s)))))

(defn go
  "Creates, stores, and starts a new application."
  []
  (create)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))


(defn get-attributes []
  (d/q '[:find ?name
         :where
         [_ :db.install/attribute ?a]
         [?a :db/ident ?name]]
       (d/db (application :db-conn))))

(defn get-pings []
  (d/q '[:find ?hash ?time
         :where
         [?e :message/hash ?hash ?t]
         [?e :message/type "PING"]
         [?t :db/txInstant ?time]]
       (d/db (application :db-conn))))
