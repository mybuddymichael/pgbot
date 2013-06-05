(ns pgbot.output
  (:require [pgbot.messages :use [compose]]))

(defn print-messages
  [_ & messages]
  (doseq [m messages]
    (println (compose m))))

(defn log
  "Log a string to a preferred output."
  [_ & messages]
  (doseq [m messages]
    (spit "/tmp/pgbot.log"
          (str (java.util.Date.) " : " (compose m) "\n")
          :append true)))
