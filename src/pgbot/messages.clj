(ns pgbot.messages
  "Functions for parsing and composing IRC lines.")

(defn parse
  "Takes a line and returns a constructed message map."
  [line]
  (let [[[_ prefix user uri type destination content]]
        (re-seq #"^(?:[:](([^!]+)![^@]*@(\S+)) )?(\S+)(?: (?!:)(.+?))?(?: [:](.+))?$"
                line)]
    (->> {:message/prefix prefix
          :message/user user
          :message/uri uri
          :message/type type
          :message/destination destination
          :message/content content}
         (filter #(identity (second %)))
         (apply conj {}))))

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
