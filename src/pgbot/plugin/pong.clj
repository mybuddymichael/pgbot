(ns pgbot.plugin.pong
  "PONGs the IRC server to keep the connection alive."
  (:require [clojure.test :refer [deftest is]]))

(defn run [])

(defn parse
  "Returns a PONG string if the server is PINGing."
  [line]
  (when-let [[_ server] (re-find #"^PING :(.+)" line)]
    (str "PONG :" server)))

(deftest parse-returns-a-pong-message-when-pinged
  (is (= "PONG :server-name"
         (parse "PING :server-name"))))

(deftest parse-returns-nil-when-not-being-pinged
  (is (nil? (parse "not a ping message"))))
