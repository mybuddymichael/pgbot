(ns pgbot.ping-pong
  (:require (pgbot [process :refer [PProcess]]
                   [messages :refer [map->Message]])
            [clojure.core.typed :as t]
            [clojure.core.typed.async :refer [Chan chan> go>]]
            [clojure.core.async :refer [<! >! close! alts!]]))

(t/typed-deps clojure.core.typed.async)

(t/ann-record PingPong [in := (Chan pgbot.messages.Message)
                        out := (Chan pgbot.messages.Message)
                        kill := (Chan Any)])

(defrecord PingPong [in out kill])

(extend-type PingPong
  PProcess
  (start [{:keys [in out kill] :as ping-pong}]
    (go>
      (loop [[message chan] (alts! [kill in] :priority true)]
        (when (not= chan kill)
          (when (= (message :type) "PING")
            (>! out (map->Message {:type "PONG"
                                   :content (message :content)})))
          (recur (alts! [kill in] :priority true)))))
    ping-pong)
  (stop [{:keys [kill] :as ping-pong}]
    (close! kill)
    ping-pong))

(defn ->PingPong []
  (PingPong. (chan> pgbot.messages.Message)
             (chan> pgbot.messages.Message)
             (chan> Any)))
