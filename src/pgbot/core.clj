(ns pgbot.core
  "A simple IRC bot."
  (:gen-class)
  (:require [taoensso.timbre :as timbre :refer [info]]
            (pgbot application)
            [clojure.core.async :refer [<!!]]))


(timbre/set-config! [:timestamp-pattern] "yyyy-MMM-dd HH:mm:ss.SS ZZ")
(timbre/set-config! [:appenders :spit :enabled?] true)
(timbre/set-config! [:appenders :standard-out :enabled?] false)
(timbre/set-config! [:shared-appender-config :spit-filename] "/tmp/pgbot.log")

(defn -main
  "Start pgbot. This will block until the connection closes, at which
   point it will automatically try to reconnect."
  [host port nick channel web-server-port]
  (info "Starting pgbot.")
  (let [application (-> (pgbot.application/create
                          {:host host :port port :nick nick :channel channel
                           :web-server-port web-server-port})
                        pgbot.application/start)]
    (info "Pgbot started.")
    (<!! (get-in application [:connection :dead]))
    (info "Connection died, stopping pgbot.")
    (pgbot.application/stop application)
    (recur host port nick channel web-server-port)))
