(ns pgbot.recorder
  (:require [clojure.core.async :as async]
            [taoensso.timbre :refer [info]]
            [pgbot.lifecycle :refer [Lifecycle]]))

(defrecord Recorder [db-conn in kill]
  Lifecycle
  (start [recorder]
    (async/thread
      (let [alts-fn #(async/alts!! [kill in] :priority true)]
        (loop [[message chan] (alts-fn)]
          (when (not= chan kill)
            (info "Recording message" (hash message))
            (recur (alts-fn))))))
    (info "Recorder started.")
    recorder)
  (stop [recorder]
    (async/close! kill)
    (info "Recorder stopped.")
    recorder))

(defn create [db-conn]
  (Recorder. db-conn (async/chan) (async/chan)))
