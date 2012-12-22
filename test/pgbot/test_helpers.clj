(ns pgbot.test-helpers
  "Custom helper functions for writing tests."
  (require [clojure.test :refer [deftest deftest- is]]))

(defmacro deftest*
  "Create a test function using `clojure.test/deftest`, but using a
  string instead of a symbol to name the test."
  [name & body]
  (let [test-name
        (-> name
          (clojure.string/lower-case)
          (clojure.string/replace #"\W" "-")
          (clojure.string/replace #"-+" "-")
          (clojure.string/replace #"-$" "")
          (symbol))]
    `(deftest ~test-name ~@body)))

(defmacro deftest-*
  "Same as deftest*, but creates a private function."
  [name & body]
  (let [test-name
        (-> name
          (clojure.string/lower-case)
          (clojure.string/replace #"\W" "-")
          (clojure.string/replace #"-+" "-")
          (clojure.string/replace #"-$" "")
          (symbol))]
    `(deftest- ~test-name ~@body)))

(deftest- test-pgbot-test-defines-tests
  (let [_ (pgbot-test "this is my test")]
    (is (clojure.test/function? this-is-my-test))))
