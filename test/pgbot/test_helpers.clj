(ns pgbot.test-helpers
  "Custom helper functions for writing tests."
  (require [clojure.test :refer [deftest deftest- is]]))

(defmacro pgbot-test
  "Create a test function using `clojure.test/deftest`, but using a
  string instead of a symbol to name the test."
  [name & body]
  (let [test-name (symbol (clojure.string/replace name #"\W" "-"))]
    `(deftest ~test-name ~@body)))

(deftest- test-pgbot-test-defines-tests
  (let [_ (pgbot-test "this is my test")]
    (is (clojure.test/function? this-is-my-test))))
