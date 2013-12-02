(ns pgbot.dispatcher
  "Dispatches incoming and outgoing Messages across various channels."
  (:require [clojure.core.async :as async]
            [taoensso.timbre :refer [info]]
            (pgbot [lifecycle :refer [Lifecycle]])))

(defn put-on-all-chans!! [chans x]
  "Puts x on chans, using alts!!. Puts are made as soon as any channel
   is available. Blocks until all operations are complete. Returns nil."
  (when (seq chans)
    (let [put-operations (map vector chans (repeat x))
          [_ success-chan] (async/alts!! put-operations)]
      (recur x (remove #{success-chan} chans)))))

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
            (put-to-all-chans!! message in-chans)
            (info "Incoming message" (hash message) "dispatched to all in-chans.")
            (recur (alts-fn))))))
    (async/thread
      (let [alts-fn #(async/alts!! (flatten [kill out-chans]) :priority true)]
        (loop [[message chan] (alts-fn)]
          (when (not= chan kill)
            (async/>!! outgoing message)
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
