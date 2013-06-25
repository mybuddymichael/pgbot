(ns pgbot.core
  "A simple IRC bot."
  (:gen-class)
  (:require (pgbot system)))

(defn -main [host port nick channel git-listener-port])
