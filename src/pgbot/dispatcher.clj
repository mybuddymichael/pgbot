(ns pgbot.dispatcher
  "Dispatches incoming and outgoing Messages across various channels."
  (:require [clojure.core.async :as async]
            [taoensso.timbre :refer [info]]
            (pgbot [lifecycle :refer [Lifecycle]])))

(defrecord Dispatcher [incoming
                       outgoing
                       in-chans
                       out-chans
                       kill]
  Lifecycle
  (start [dispatcher]
    (async/thread
      (let [alts-fn #(async/alts!! [kill incoming] :priority true)]
        (loop [[message chan] (alts-fn)]
          (when (not= chan kill)
            (doseq [c in-chans]
              (async/put! c message))
            (info "Incoming message" (hash message) "dispatched to all in-chans.")
            (recur (alts-fn))))))
    (async/thread
      (let [alts-fn #(async/alts!! (flatten [kill out-chans]) :priority true)]
        (loop [[message chan] (alts-fn)]
          (when (not= chan kill)
            (async/put! outgoing message)
            (info "Outgoing message" (hash message) "dispatched to out.")
            (recur (alts-fn))))))
    (info "Dispatcher started.")
    dispatcher)
  (stop [dispatcher]
    (async/close! kill)
    (info "Dispatcher stopped.")
    dispatcher))

(defn create [{:keys [incoming outgoing in-chans out-chans]}]
  (Dispatcher. incoming outgoing in-chans out-chans (async/chan)))
