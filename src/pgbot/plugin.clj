(ns pgbot.plugin
  "Handles registration and keeping of pgbot plugins.")

(def plugins (agent #{}))

(defn register
  "Register a plugin ns symbol for use by pgbot.core."
  [plugin]
  (send plugins conj plugin))
