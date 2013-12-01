(ns pgbot.messages
  "Functions for parsing and composing IRC lines.")

(defn- parse
  "Takes a line and a map of options and returns a constructed message
   map. Options are currently just a :source that is one of either
   :incoming or :outgoing."
  [line {:keys [source]}]
  {:pre [source]}
  (let [[[_ prefix user uri type destination content]]
        (re-seq #"^(?:[:](([^!]+)![^@]*@(\S+)) )?(\S+)(?: (?!:)(.+?))?(?: [:](.+))?$"
                line)]
    {:prefix (some-> prefix str)
     :user (some-> user str)
     :uri (some-> uri str)
     :type (some-> type str)
     :destination (some-> destination str)
     :content (some-> content str)
     :source source}))

(defn parse-incoming
  "Uses parse to generate an incoming message map."
  [line]
  (parse line {:source :incoming}))

(defn parse-outgoing
  "Uses parse to generate an outgoing message map."
  [line]
  (parse line {:source :outgoing}))

(defn compose
  "Takes a Message and returns a reconstructed line."
  [{:keys [prefix type destination content]}]
  (let [prefix (when prefix (str ":" prefix " "))
        type (if destination (str type " ") type)
        content (when content (str " :" content))]
    (str prefix type destination content)))
