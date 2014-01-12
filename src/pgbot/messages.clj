(ns pgbot.messages
  "Functions for parsing and composing IRC lines."
  (:require [clj-time.core :as clj-time]))

(defn ^:private filter-into-map
  "Takes a collection of logical key-value pairs and returns a new map
   where each (pred value) returns true."
  [pred coll]
  (into {} (filter (fn [[_ v]] (pred v)) coll)))

(defn parse
  "Takes a line and returns a constructed message map."
  ([line] (parse line (clj-time/now)))
  ([line instant]
   (let [[[_ prefix user uri type destination content]]
         (re-seq #"^(?:[:](([^!]+)![^@]*@(\S+)) )?(\S+)(?: (?!:)(.+?))?(?: [:](.+))?$"
                 line)]
     (filter-into-map identity
                      {:message/time instant
                       :message/prefix prefix
                       :message/user user
                       :message/uri uri
                       :message/type type
                       :message/destination destination
                       :message/content content}))))

(defn compose
  "Takes a Message and returns a reconstructed line."
  [{prefix :message/prefix
    type :message/type
    content :message/content
    destination :message/destination}]
  (let [prefix (when prefix (str ":" prefix " "))
        type (if destination (str type " ") type)
        content (when content (str " :" content))]
    (str prefix type destination content)))

(defn privmsg
  "Generates an outgoing message map to go to the provided channel or
   user with the provided content."
  [destination content]
  (parse (str "PRIVMSG " destination " :" content)))
