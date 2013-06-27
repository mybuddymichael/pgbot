(ns pgbot.events-test
  (:require (pgbot [test-helpers :refer [deftest*]]
                   events)
            [clojure.test :refer [is]]))

(deftest* "register returns a new connection with added functions"
  (let [the-fn (fn [] nil)]
    (is (= {:events {:incoming #{the-fn}}}
           (pgbot.events/register {:events {}} [:incoming] [the-fn])))))

(deftest* "register can take multiple events and multiple functions"
  (let [fn1 (fn [] nil)
        fn2 (fn [] nil)]
    (is (= {:events {:incoming #{fn1 fn2}
                     :outgoing #{fn1 fn2}}}
           (pgbot.events/register {:events {}}
                                  [:incoming :outgoing] [fn1 fn2])))))

(deftest* "trigger calls the functions for the event"
  (let [p1 (promise)
        p2 (promise)
        connection (pgbot.events/register {:events {}}
                                          [:incoming]
                                          [(fn [& _] (deliver p1 4))
                                           (fn [& _] (deliver p2 2))])]
    (pgbot.events/trigger connection :incoming)
    (is (= @p1 4)
        (= @p2 2))))
