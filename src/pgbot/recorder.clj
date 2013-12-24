(ns pgbot.recorder
  (:require [clojure.core.async :as async]
            [datomic.api :as d]
            [taoensso.timbre :refer [info]]
            [pgbot.lifecycle :refer [Lifecycle]]))

(defrecord Recorder [db-conn in]
  Lifecycle
  (start [recorder]
    (async/thread
      (loop [message (async/<!! in)]
        (when message
          (info "Recording message" (hash message))
          (recur (async/<!! in)))))
    (info "Recorder started.")
    recorder)
  (stop [recorder]
    (async/close! in)
    (info "Recorder stopped.")
    recorder))

(defn create [buffer-size db-conn]
  (Recorder. db-conn (async/chan buffer-size)))
