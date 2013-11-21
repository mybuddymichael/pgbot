(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [clojure.repl :refer :all]
            [clojure.pprint :refer [pprint]]
            [clojure.test :refer [run-tests]]
            [clojure.core.typed :as t]
            (pgbot application debug)))

(def application nil)

(defn check []
  (t/check-ns 'pgbot.core))

(defn create
  "Creates and stores a new application instance."
  []
  (alter-var-root #'application (constantly (pgbot.application/create
                                              {:host "irc.freenode.net"
                                               :port "6667"
                                               :nick "pgbottest"
                                               :channel "##pgbottest"
                                               :commit-server-port "8080"}))))

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
