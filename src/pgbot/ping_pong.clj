(ns pgbot.ping-pong
  (:require [clojure.core.async :refer [chan go <! >! >!! alts!]]))

(defn create
  "Creates and returns a system to check for ping messages from the IRC
   server."
  [out]
  {:in (chan)
   :out out
   :stop (chan)})

(defn start
  "Runs side effects to begin listening for ping-pong messages inside a
   go process. Returns the system."
  [{:keys [in out stop] :as ping-pong}]
  (go
    (loop [[message chan] (alts! [stop in] :priority true)]
      (when (and (not= chan stop)
                 (= (message :type) "PING"))
        (>! out {:type "PONG"
                 :content (message :content)})
        (recur (alts! [stop in] :priority true)))))
  ping-pong)

(defn stop
  "Runs side effects to stop the ping-pong system. Returns the stopped
   system."
  [{:keys [stop] :as ping-pong}]
  (>!! stop true)
  ping-pong)
