(ns pgbot.dispatcher
  "Dispatches incoming and outgoing Messages across various channels."
  (:require [clojure.core.async :as async]
            [clojure.core.typed :as t :refer [ann ann-record doseq> Seqable]]
            [clojure.core.typed.async :as typed.async :refer [Chan]]
            (pgbot [lifecycle :refer [Lifecycle]]
                   [messages :refer [Message]])))

(t/typed-deps clojure.core.typed.async)

(t/tc-ignore
(ann-record Dispatcher [incoming :- (Chan Message)
                        outgoing :- (Chan Message)
                        in-chans :- (Seqable (Chan Message))
                        out-chans :- (Seqable (Chan Message))
                        kill :- (Chan Nothing)])
(defrecord Dispatcher [incoming outgoing in-chans out-chans stop]
  Lifecycle
  (start [{:keys [in-chans out-chans stop] :as dispatcher}]
    (async/thread
      (let [alts-fn #(async/alts!! [stop incoming] :priority true)]
        (loop [[message chan] (alts-fn)]
          (when (not= chan stop)
            (doseq> [c :- (Chan Message), in-chans]
              (async/put! c message))
            (recur (alts-fn))))))
    (async/thread
      (let [alts-fn #(async/alts!! (flatten [stop out-chans]) :priority true)]
        (loop [[message chan] (alts-fn)]
          (when (not= chan stop)
            (async/put! outgoing message)
            (recur (alts-fn))))))
    dispatcher)
  (stop [{:keys [kill] :as dispatcher}]
    (async/close! kill)
    dispatcher))

(ann map->Dispatcher [(HMap :mandatory {:incoming (Chan Message)
                                        :outgoing (Chan Message)
                                        :in-chans (Seqable (Chan Message))
                                        :out-chans (Seqable (Chan Message))})
                      -> Dispatcher])
(defn map->Dispatcher [{:keys [incoming outgoing in-chans out-chans]}]
  (Dispatcher. incoming outgoing in-chans out-chans (typed.async/chan> Nothing))))
