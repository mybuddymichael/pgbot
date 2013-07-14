(ns pgbot.ping-pong
  (:require (pgbot [lifecycle :refer [Lifecycle]])
            [clojure.core.async :refer [chan go <! >! close! alts!]]))

(defrecord PingPong [in out stop])

(extend-type PingPong
  Lifecycle
  (start [{:keys [in out stop] :as ping-pong}]
    (go
      (loop [[message chan] (alts! [stop in] :priority true)]
        (when (and (not= chan stop)
                   (= (message :type) "PING"))
          (>! out {:type "PONG"
                   :content (message :content)})
          (recur (alts! [stop in] :priority true)))))
    ping-pong)
  (stop [{:keys [stop] :as ping-pong}]
    (close! stop)
    ping-pong))

(defn ->PingPong [] (PingPong. (chan) (chan) (chan)))
