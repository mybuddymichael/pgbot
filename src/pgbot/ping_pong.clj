(ns pgbot.ping-pong
  (:require (pgbot [lifecycle :refer [Lifecycle]]
                   [messages :refer [Message]])
            [clojure.core.typed :as t :refer [ann ann-record Map Nilable Seq typed-deps]]
            [clojure.core.typed.async :refer [Chan chan> go>]]
            [clojure.core.async :refer [<! >! close! alts!]])
  (:import [clojure.lang Keyword]))

(t/typed-deps clojure.core.typed.async)

(ann responses (Map Keyword [Message -> (Nilable Message)]))
(def responses
  {:ping-pong (fn [m] (when (= (:type m) "PING")
                        {:type "PONG"
                         :destination nil
                         :content (:content m)}))})

(t/ann-record PingPong [in := (Chan Message)
                        out := (Chan Message)
                        kill := (Chan Any)])
(defrecord PingPong [in out kill]
  Lifecycle
  (start [{:keys [in out kill] :as ping-pong}]
    (go>
      (loop [[message chan] (alts! [kill in] :priority true)]
        (when (not= chan kill)
          (when-let [pong-message (get-pong message)]
            (>! out pong-message))
          (recur (alts! [kill in] :priority true)))))
    ping-pong)
  (stop [{:keys [kill] :as ping-pong}]
    (close! kill)
    ping-pong))

(defn ->PingPong []
  (PingPong. (chan> Message)
             (chan> Message)
             (chan> Any)))
