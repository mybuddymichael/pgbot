(ns pgbot.ping-pong
  (:require [clojure.core.async :refer [chan go <! >! close!]]))

(defn create
  "Creates and returns a system to check for ping messages from the IRC
   server."
  [out]
  {:in (chan)
   :out out
   :loop nil})

(defn start
  "Runs side effects to begin listening for ping-pong messages inside a
   go process. Returns a new system containing the channels and the
   started process."
  [{:keys [in out] :as ping-pong}]
  (assoc ping-pong
         :loop
         (go
           (loop [message (<! in)]
             (when (= (message :type) "PING")
               (>! out {:type "PONG"
                        :content (message :content)}))
             (recur (<! in))))))

(defn stop
  "Runs side effects to stop the ping-pong system. Returns the stopped
   system."
  [{:keys [loop] :as ping-pong}]
  (assoc ping-pong
         :loop
         (close! loop)))
