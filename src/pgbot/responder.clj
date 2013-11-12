(ns pgbot.responder
  (:require (pgbot [lifecycle :refer [Lifecycle]]
                   [messages :refer [Message]])
            [clojure.core.typed :as t :refer [ann ann-record Map Nilable Seq typed-deps]]
            [clojure.core.typed.async :refer [Chan chan> go>]]
            [clojure.core.async :refer [<! >! close! alts!]])
  (:import [clojure.lang Keyword]))

(typed-deps clojure.core.typed.async)

(ann responses (Map Keyword [Message -> (Nilable Message)]))
(def responses
  {:ping-pong (fn [m] (when (= (:type m) "PING")
                        {:type "PONG"
                         :destination nil
                         :content (:content m)}))})

(ann-record Responder [in := (Chan Message)
                      out := (Chan Message)
                      kill := (Chan Any)])
(defrecord Responder [in out kill]
  Lifecycle
  (start [{:keys [in out kill] :as responder}]
    (go>
      (loop [[message chan] (alts! [kill in] :priority true)]
        (when (not= chan kill)
          (as-> (map #(apply % [message]) (vals responses)) rs
            (filter identity rs)
            (doseq [r rs] (>! out r)))
          (recur (alts! [kill in] :priority true)))))
    responder)
  (stop [{:keys [kill] :as responder}]
    (close! kill)
    responder))

(defn ->Responder []
  (Responder. (chan> Message)
              (chan> Message)
              (chan> Any)))
