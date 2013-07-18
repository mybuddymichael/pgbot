(ns pgbot.logger
  (:require (pgbot [lifecycle :refer [Lifecycle]]
                   [messages :refer [compose]])
            [clojure.core.async :refer [alts! chan go <! close!]]))

(defrecord Logger [in out-listener stop])

(extend-type Logger
  Lifecycle
  (start [{:keys [in out-listener stop] :as logger}]
    (go
      (loop [[message chan] (alts! [stop in out-listener] :priority true)]
        (when (not= chan stop)
          (spit "/tmp/pgbot.log"
                (str (java.util.Date.) " : " (compose message) "\n")
                :append true)
          (recur (alts! [stop in out-listener] :priority true)))))
    logger)
  (stop [{:keys [stop] :as logger}]
    (close! stop)
    logger))

(defn ->Logger []
  (Logger. (chan) (chan) (chan)))
