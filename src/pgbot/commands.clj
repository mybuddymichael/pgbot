(ns pgbot.commands
  "Functions that map to common IRC commands."
  (:require pgbot.io))

(defn join
  "Join a channel."
  [connection channel]
  (pgbot.io/send-message connection "JOIN" channel))
