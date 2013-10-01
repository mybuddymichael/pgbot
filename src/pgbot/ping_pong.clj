(ns pgbot.ping-pong
  (:require (pgbot [process :refer [PProcess]]
                   [messages :refer [map->Message]])
            [clojure.core.typed :as t]
            [clojure.core.typed.async :refer [Chan chan> go>]]
            [clojure.core.async :refer [<! >! close! alts!]])
  (:import pgbot.messages.Message))

(t/typed-deps clojure.core.typed.async)

(t/ann get-pong
       [Message -> (t/Nilable Message)])
(defn get-pong [m]
  (when (= (:type m) "PING")
    (map->Message {:type "PONG"
                   :content (:content m)
                   :destination nil
                   :prefix nil})))

(t/ann-record PingPong [in := (Chan Message)
                        out := (Chan Message)
                        kill := (Chan Any)])

(defrecord PingPong [in out kill])

(extend-type PingPong
  PProcess
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
