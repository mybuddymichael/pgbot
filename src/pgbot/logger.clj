(ns pgbot.logger
  (:require (pgbot [lifecycle :refer [Lifecycle]]
                   [messages :refer [compose]])
            [clojure.core.async :refer [alts! chan go <! close!]]))

(defrecord Logger [out-listener stop])

(extend-type Logger
  Lifecycle
  (start [{:keys [out-listener stop] :as Logger}]
    (go (loop [[message chan] (alts! [stop out-listener] :priority true)]
          (when (not= chan stop)
            (println (compose message))
            (spit "/tmp/pgbot.log"
                  (str (java.util.Date.) " : " (compose message) "\n")
                  :append true))
          (recur (alts! [stop out-listener] :priority true))))
    Logger)
  (stop [{:keys [stop] :as Logger}]
    (close! stop)
    Logger))

(defn ->Logger []
  (Logger. (chan) (chan)))
