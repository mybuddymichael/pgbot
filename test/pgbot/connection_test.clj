(ns pgbot.connection-test
  (:require (pgbot [test-helpers :refer [deftest*]]
                   [connection :as connection]
                   [lifecycle :as lifecycle])
            [clojure.core.async :refer [chan go <!!]]
            [clojure.test :refer [is]]))

(deftest* "start begins placing incoming messages on in channels"
  (let [in-chans [(chan)]
        c (-> (connection/create "irc.freenode.net" 6667 "pgbottest"
                                 "##pgbottest" in-chans [])
              (lifecycle/start))]
    (is (<!! (first in-chans)))
    (lifecycle/stop c)))
