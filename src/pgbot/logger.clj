(ns pgbot.logger
  (:require (pgbot [process :refer [PProcess]]
                   [messages :refer [compose Message]])
            [clojure.core.typed :as t]
            [clojure.core.typed.async :as a :refer [Chan chan> go>]]
            [clojure.core.async :refer [alts! go <! close!]]))

(t/typed-deps clojure.core.typed.async)

(t/ann-record Logger [in := (Chan Message)
                      out-listener := (Chan Message)
                      kill := (Chan Any)])

(defrecord Logger [in out-listener kill])

(extend-type Logger
  PProcess
  (start [{:keys [in out-listener kill] :as logger}]
    (go>
      (loop [[message chan] (alts! [kill in out-listener] :priority true)]
        (when (not= chan kill)
          (spit "/tmp/pgbot.log"
                (str (java.util.Date.) " : " (compose message) "\n")
                :append true)
          (recur (alts! [kill in out-listener] :priority true)))))
    logger)
  (stop [{:keys [kill] :as logger}]
    (close! kill)
    logger))

(defn ->Logger []
  (Logger. (chan> Message)
           (chan> Message)
           (chan> Any)))
