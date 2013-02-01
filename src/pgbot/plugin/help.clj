(ns pgbot.plugin.help
  "Displays information about pgbot.")

(def keywords #{"help" "source" "info" "information"})

(defn run [connection])

(defn parse [connection line]
  (when (some #(re-find (re-pattern %) line)
              keywords)
    (str
      "PRIVMSG " (connection :channel)
      " :pgbot is an open source project by Michael Hanson."
      " You can view the source at https://github.com/mybuddymichael/pgbot.")))
