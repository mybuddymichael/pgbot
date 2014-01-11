(ns pgbot.recorder
  (:require [clojure.core.async :as async]
            [datomic.api :as d]
            [taoensso.timbre :refer [debug info error]]
            [pgbot.lifecycle :refer [Lifecycle]]))

(defn ^:private message->transaction
  "Converts a message map to a Datomic transaction list."
  ([message] (message->transaction message (d/tempid :db.part/user)))
  ([message tempid]
   (list (merge {:db/id tempid
                 :message/hash (hash message)}
                message))))

(defn ^:private record-message [db-conn message]
  (info "Recording message" (hash message))
  (let [transaction (message->transaction message)]
    (debug "Transaction is" transaction)
    (try @(d/transact db-conn transaction)
      (catch Exception e (error e)))
    (info "Done recording message" (hash message))))

(defrecord Recorder [db-conn in]
  Lifecycle
  (start [recorder]
    (async/thread
      (loop [message (async/<!! in)]
        (when message
          (record-message db-conn message)
          (recur (async/<!! in)))))
    (info "Recorder started.")
    recorder)
  (stop [recorder]
    (async/close! in)
    (info "Recorder stopped.")
    recorder))

(defn create [buffer-size db-conn]
  (Recorder. db-conn (async/chan buffer-size)))
