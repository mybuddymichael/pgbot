(ns pgbot.responder
  (:require (pgbot [lifecycle :refer [Lifecycle]]
                   [messages :as messages])
            [clojure.core.async :as async]
            [taoensso.timbre :refer [info]]))

(def responses
  "A map of keywords to functions that take a message and optionally
   return a response message if the message satisfies arbitrary
   requirements."
  {:ping-pong (fn [m] (when (= (:type m) "PING")
                        (messages/parse (str "PONG :" (:content m)))))})

(defrecord Responder [kill in out]
  Lifecycle
  (start [responder]
    (async/go
      (loop [[message chan] (async/alts! [kill in] :priority true)]
        (when (not= chan kill)
          (info "Processing message" (hash message))
          (as-> (map #(apply % [message]) (vals responses)) rs
            (filter identity rs)
            (doseq [r rs]
              (async/>! out r)
              (info "Message" (hash r) "placed on out.")))
          (info "Done processing message" (hash message))
          (recur (async/alts! [kill in] :priority true)))))
    (info "Responder started.")
    responder)
  (stop [responder]
    (async/close! kill)
    (info "Responder stopped.")
    responder))

(defn create
  "Initialize a responder."
  [buffer-size]
  (apply ->Responder (map (fn [_] (async/chan buffer-size)) (range 3))))
