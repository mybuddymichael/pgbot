(ns pgbot.dispatcher
  "Dispatches incoming and outgoing Messages across various channels."
  (:require [clojure.core.async :as async]
            [clojure.core.typed :as t :refer [ann ann-record doseq> Seqable]]
            [clojure.core.typed.async :as typed.async :refer [Chan]]
            [taoensso.timbre :refer [info]]
            (pgbot [lifecycle :refer [Lifecycle]]
                   [messages :refer [Message]])))

(t/typed-deps clojure.core.typed.async)

(ann-record Dispatcher [incoming :- (Chan Message)
                        outgoing :- (Chan Message)
                        in-chans :- (Seqable (Chan Message))
                        out-chans :- (Seqable (Chan Message))
                        kill :- (Chan Nothing)])
(defrecord Dispatcher [incoming outgoing in-chans out-chans kill]
  Lifecycle
  (start [dispatcher]
    (t/tc-ignore
    (async/thread
      (let [alts-fn #(async/alts!! [kill incoming] :priority true)]
        (loop [[message chan] (alts-fn)]
          (when (not= chan kill)
            (doseq> [c :- (Chan Message), in-chans]
              (async/put! c message))
            (info "Incoming message" (:uuid message) "placed on in-chans.")
            (recur (alts-fn))))))
    (async/thread
      (let [alts-fn #(async/alts!! (flatten [kill out-chans]) :priority true)]
        (loop [[message chan] (alts-fn)]
          (when (not= chan kill)
            (async/put! outgoing message)
            (info "Outgoing message" (:uuid message) "placed on outgoing.")
            (recur (alts-fn))))))
    (info "Dispatcher started."))
    dispatcher)
  (stop [dispatcher]
    (async/close! kill)
    dispatcher))

(ann create [(HMap :mandatory {:incoming (Chan Message)
                               :outgoing (Chan Message)
                               :in-chans (Seqable (Chan Message))
                               :out-chans (Seqable (Chan Message))})
             -> Dispatcher])
(defn create [{:keys [incoming outgoing in-chans out-chans]}]
  (Dispatcher. incoming outgoing in-chans out-chans (typed.async/chan> Nothing)))
