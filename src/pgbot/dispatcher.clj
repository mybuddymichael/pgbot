(ns pgbot.dispatcher
  (require [clojure.core.async :refer [chan go <! >! >!!]]))

(defn create [in channels]
  {:in in
   :channels channels
   :stop (chan)})

(defn start [{:keys [in channels stop] :as dispatcher}]
  (go
    (loop [[message channel] (alts! [stop in] :priority true)]
      (when (not= channel stop)
        (doseq [c channels]
          (go (>! c message)))
        (recur (alts! [stop in] :priority true)))))
  dispatcher)

(defn stop [{:keys [stop] :as dispatcher}]
  (>!! stop true)
  dispatcher)
