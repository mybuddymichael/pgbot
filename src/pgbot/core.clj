(ns pgbot.core
  "A simple IRC bot."
  (:gen-class)
  (:require [taoensso.timbre :as timbre :refer [info]]
            (pgbot application)
            [clojure.core.typed :as t]
            [clojure.core.async :refer [<!!]]))

(t/tc-ignore
(timbre/set-config! [:timestamp-pattern] "yyyy-MMM-dd HH:mm:ss.SS ZZ")
(timbre/set-config! [:appenders :spit :enabled?] true)
(timbre/set-config! [:appenders :standard-out :enabled?] false)
(timbre/set-config! [:shared-appender-config :spit-filename] "/tmp/pgbot.log")
)

(t/ann -main [String String String String String -> nil])
(defn -main
  "Start pgbot. This will block until the connection closes, at which
   point it will automatically try to reconnect."
  [host port nick channel commit-server-port]
  (t/tc-ignore (info "Starting pgbot."))
  (let [application (-> (pgbot.application/create
                          {:host host :port port :nick nick :channel channel
                           :commit-server-port commit-server-port})
                        pgbot.application/start)]
    (t/tc-ignore (info "Pgbot started."))
    (<!! (-> application :connection :kill))
    (t/tc-ignore (info "Connection died, stopping pgbot."))
    (pgbot.application/stop application)
    (recur host port nick channel commit-server-port)))
