(ns pgbot.logger
  (:require (pgbot [messages :refer [compose]]
                   events)
            [clojure.core.async :refer [alts! chan go <!]]))

(defn create []
  {:in (chan)
   :stop (chan)})

(defn start
  "Log a string to a file."
  [{:keys [in stop] :as logger}]
  (go
    (loop [[message chan] (alts! [stop in] :priority true)]
      (when (not= chan stop)
        (spit "/tmp/pgbot.log"
              (str (java.util.Date.) " : " (compose message) "\n")
              :append true))
      (recur (alts! [stop in] :priority true))))
  logger)

(defn stop
  [{:keys [stop] :as logger}]
  (>!! stop true)
  logger)
