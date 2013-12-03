(ns pgbot.messages
  "Functions for parsing and composing IRC lines.")

(defn parse
  "Takes a line and returns a constructed message map."
  [line]
  (let [[[_ prefix user uri type destination content]]
        (re-seq #"^(?:[:](([^!]+)![^@]*@(\S+)) )?(\S+)(?: (?!:)(.+?))?(?: [:](.+))?$"
                line)]
    {:prefix (some-> prefix str)
     :user (some-> user str)
     :uri (some-> uri str)
     :type (some-> type str)
     :destination (some-> destination str)
     :content (some-> content str)}))

(defn compose
  "Takes a Message and returns a reconstructed line."
  [{:keys [prefix type destination content]}]
  (let [prefix (when prefix (str ":" prefix " "))
        type (if destination (str type " ") type)
        content (when content (str " :" content))]
    (str prefix type destination content)))

(defn privmsg
  "Generates an outgoing message map to go to the provided channel or
   user with the provided content."
  [destination content]
  (parse (str "PRIVMSG " destination " :" content)))
