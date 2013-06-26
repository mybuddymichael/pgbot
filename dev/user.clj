(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [clojure.repl :refer :all]
            [clojure.test :refer [run-tests]]
            (pgbot system debug)))

(def system nil)

(defn create
  "Creates and stores a new application instance."
  []
  (alter-var-root #'system (constantly (pgbot.system/create))))

(defn start
  "Starts the current application."
  []
  (alter-var-root #'system pgbot.system/start))

(defn stop
  "Shuts down the application and releases resources."
  []
  (alter-var-root #'system (fn [s] (when s (pgbot.system/stop s)))))

(defn go
  "Creates, stores, and starts a new application."
  []
  (create)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))
