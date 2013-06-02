(ns pgbot.messages)

(defn parse
  "Takes a message string and returns a map of the message properties."
  [message]
  (let [[_ prefix type destination content]
        (re-matches #"^(?:[:](\S+) )?(\S+)(?: (?!:)(.+?))?(?: [:](.+))?$"
                    message)]
    {:prefix prefix
     :type type
     :destination destination
     :content content}))

(defn compose
  "Takes a message map and returns a reconstructed message string."
  [{:keys [prefix type destination content]}]
  (let [prefix (when prefix (str ":" prefix " "))
        type (if destination (str type " ") type)
        content (when content (str " :" content))]
    (str prefix type destination content)))
