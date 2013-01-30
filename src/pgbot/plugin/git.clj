(ns pgbot.plugin.git
  "Watches for git pushes and displays notifications."
  (:require clojure.java.io))

(defn run []
  (let [git-push-log-maps (->> (file-seq (clojure.java.io/file "/tmp"))
                               (map str)
                               (filter #(re-matches #".*\.edn" %))
                               (map slurp)
                               (map read-string))]
    (->> (map (fn [x] (:messages x)) git-push-log-maps)
         (flatten))))

(defn parse [line])
