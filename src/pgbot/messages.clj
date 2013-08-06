(ns pgbot.messages
  "Functions for parsing and composing IRC lines."
  (:require [clojure.core.typed :as t]))

(t/ann-record Message [prefix := (U String nil)
                       type := (U String nil)
                       destination := (U String nil)
                       content := (U String nil)])

(defrecord Message [prefix type destination content])

(t/ann parse [String -> Message])
(defn parse
  "Takes a line and returns a Message."
  [line]
  (let [[[_ prefix type destination content]]
        (re-seq #"^(?:[:](\S+) )?(\S+)(?: (?!:)(.+?))?(?: [:](.+))?$" line)]
    (map->Message {:prefix prefix
                   :type type
                   :destination destination
                   :content content})))

(t/ann compose [Message -> String])
(defn compose
  "Takes a Message and returns a reconstructed line."
  [{:keys [prefix type destination content]}]
  (let [prefix (when prefix (str ":" prefix " "))
        type (if destination (str type " ") type)
        content (when content (str " :" content))]
    (str prefix type destination content)))
