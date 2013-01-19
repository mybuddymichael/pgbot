(ns pgbot.test-helpers
  (:require [clojure.test :refer [is]]))

(defn- string->symbol
  "Converts a string to a hyphen-separated symbol."
  [string]
  (-> string
      clojure.string/lower-case
      (clojure.string/replace #"\W" "-")
      (clojure.string/replace #"-+" "-")
      (clojure.string/replace #"-$" "")
      symbol))

(defmacro deftest*
  "Define a test using deftest, but with a string as the name instead of
   a symbol."
  [name-string & body]
  (let [name-symbol (string->symbol name-string)]
    `(clojure.test/deftest ~name-symbol ~@body)))

(defmacro deftest-*
  "Define a private test using deftest-, but with a string as the name
   instead of a symbol."
  [name-string & body]
  (let [name-symbol (string->symbol name-string)]
    `(clojure.test/deftest- ~name-symbol ~@body)))

(clojure.test/deftest- deftest*-creates-a-test-with-a-symbol-name
  (is (= '(clojure.test/deftest this-is-a-test (clojure.test/is true))
         (macroexpand-1 `(deftest* "this is a test" (is true))))))

(clojure.test/deftest- deftest-*-creates-a-private-test-with-a-symbol-name
  (is (= '(clojure.test/deftest- this-is-a-test (clojure.test/is true))
         (macroexpand-1 `(deftest-* "this is a test" (is true))))))
