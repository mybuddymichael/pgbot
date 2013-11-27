(ns pgbot.messages
  "Functions for parsing and composing IRC lines."
  (:require [taoensso.timbre :refer [info debug]]))

(defn parse
  "Takes a line and returns a Message."
  [line]
  (let [[[_ prefix user uri type destination content]]
        (re-seq #"^(?:[:](([^!]+)![^@]*@(\S+)) )?(\S+)(?: (?!:)(.+?))?(?: [:](.+))?$"
                line)
        message {:prefix (some-> prefix str)
                 :user (some-> user str)
                 :uri (some-> uri str)
                 :type (some-> type str)
                 :destination (some-> destination str)
                 :content (some-> content str)
                 :uuid (java.util.UUID/randomUUID)}]
    (info "Parsed message" (:uuid message))
    (debug message)
    message))

(defn compose
  "Takes a Message and returns a reconstructed line."
  [{:keys [prefix type destination content]}]
  (let [prefix (when prefix (str ":" prefix " "))
        type (if destination (str type " ") type)
        content (when content (str " :" content))]
    (str prefix type destination content)))
