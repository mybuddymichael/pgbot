(ns pgbot.messages
  "Functions for parsing and composing IRC lines."
  (:require [clojure.core.typed :as t]
            [taoensso.timbre :refer [info]])
  (:import java.util.UUID))

(t/non-nil-return java.util.UUID/randomUUID :all)

(t/def-alias Message
  (HMap :mandatory {:type (U String nil)
                    :destination (U String nil)
                    :uuid UUID}
        :optional {:prefix (U String nil)
                   :content (U String nil)}))

(t/ann parse [String -> Message])
(defn parse
  "Takes a line and returns a Message."
  [line]
  (let [[[_ prefix type destination content]]
        (re-seq #"^(?:[:](\S+) )?(\S+)(?: (?!:)(.+?))?(?: [:](.+))?$" line)
        message {:prefix (some-> prefix str)
                 :type (some-> type str)
                 :destination (some-> destination str)
                 :content (some-> content str)
                 :uuid (UUID/randomUUID)}]
    (t/tc-ignore (info "Parsed message" (:uuid message)))
    message))

(t/ann compose [Message -> String])
(defn compose
  "Takes a Message and returns a reconstructed line."
  [{:keys [prefix type destination content]}]
  (let [prefix (when prefix (str ":" prefix " "))
        type (if destination (str type " ") type)
        content (when content (str " :" content))]
    (str prefix type destination content)))
