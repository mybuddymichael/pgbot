(ns pgbot.connection-test
  (:require (pgbot [test-helpers :refer [deftest*]]
                   [connection :as connection])
            [clojure.core.async :refer [chan go <!!]]
            [clojure.test :refer [is]]))

(deftest* "start begins placing incoming messages on in channels"
  (let [in-chans [(chan)]
        c (-> (connection/create "irc.freenode.net" 6667 "pgbottest"
                                 "##pgbottest" in-chans [] [])
              (connection/start))]
    (is (<!! (first in-chans)))
    (connection/stop c)))
