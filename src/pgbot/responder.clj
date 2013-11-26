(ns pgbot.responder
  (:require (pgbot [lifecycle :refer [Lifecycle]]
                   [messages :as messages])
            [clojure.core.async :as async]
            [taoensso.timbre :refer [info]]))

(def responses
  "A Map of Keywords to functions that take an incoming Message and
   optionally return a response Message if the incoming Message
   satisfies arbitrary requirements."
  {:ping-pong (fn [m] (when (= (:type m) "PING")
                        (messages/parse (str "PONG :" (:content m)))))})

(defrecord Responder [in out kill]
  Lifecycle
  (start [responder]
    (async/go
      (loop [[message chan] (async/alts! [kill in] :priority true)]
        (when (not= chan kill)
          (info "Processing incoming message" (:uuid message))
          (as-> (map #(apply % [message]) (vals responses)) rs
            (filter identity rs)
            (doseq [r rs]
              (async/>! out r)
              (info "Outgoing message" (:uuid r) "placed on out.")))
          (recur (alts! [kill in] :priority true)))))
    (info "Responder started.")
    responder)
  (stop [responder]
    (async/close! kill)
    responder))

(defn create
  "Initialize a responder."
  []
  (Responder. (async/chan) (async/chan) (async/chan)))
