(ns pgbot.debug
  "Simple helpers for debugging.")

(defmacro check [x]
  "Prints the passed-in symbol plus its value."
  `(let [x# ~x]
     (println "debug:" '~x "=" x#)
     x#))
