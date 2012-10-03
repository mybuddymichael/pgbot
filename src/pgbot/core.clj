(ns pgbot.core
  (:require [pgbot.connection :as connection]))

(defn -main [host port nick & [password channel]]
  (connection/connect :host host :port port :nick nick
                      :password password :channel channel))
