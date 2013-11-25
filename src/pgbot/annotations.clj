(ns pgbot.annotations
  "Annotations for un-annotated third party code."
  (:require [clojure.core.typed :refer [ann non-nil-return]]
            [clojure.core.typed.async :refer [Chan]]))

(non-nil-return java.util.UUID/randomUUID :all)

(ann ^:no-check clojure.java.io/reader [Any -> java.io.BufferedReader])
(ann ^:no-check clojure.java.io/writer [Any -> java.io.BufferedWriter])

(ann ^:no-check clojure.core.async/put!
  (All [a] (Fn [(Chan a) a -> nil]
               [(Chan a) a [Any * -> Any] -> nil]
               [(Chan a) a [Any * -> Any] Boolean -> nil])))
