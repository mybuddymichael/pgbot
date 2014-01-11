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

(defrecord Recorder [db-conn in]
  Lifecycle
  (start [recorder]
    (async/thread
      (loop [message (async/<!! in)]
        (when message
          (info "Recording message" (hash message))
          (let [transaction
                [{:db/id #db/id [:db.part/user]
                  :message/hash (hash message)
                  :message/prefix (str (message :prefix))
                  :message/user (str (message :user))
                  :message/uri (str (message :uri))
                  :message/type (str (message :type))
                  :message/destination (str (message :destination))
                  :message/content (str (message :content))}]]
            (try @(d/transact db-conn transaction)
              (catch Exception e (error e)))
            (info "Message" (hash message) "recorded.")
            (recur (async/<!! in))))))
    (info "Recorder started.")
    recorder)
  (stop [recorder]
    (async/close! in)
    (info "Recorder stopped.")
    recorder))

(defn create [buffer-size db-conn]
  (Recorder. db-conn (async/chan buffer-size)))
