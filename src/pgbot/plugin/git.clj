(ns pgbot.plugin.git
  "Watches for git pushes and displays notifications."
  (:require clojure.java.io))

(defn run [connection]
  (let [git-push-log-maps (->> (file-seq (clojure.java.io/file "/tmp"))
                               (map str)
                               (filter #(re-matches #".*\.edn" %))
                               (map slurp)
                               (map read-string))]
    (flatten (map (fn [x] (:messages x)) git-push-log-maps))))

(defn parse [connection line])
