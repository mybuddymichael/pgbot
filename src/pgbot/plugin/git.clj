(ns pgbot.plugin.git
  "Watches for git pushes and displays notifications."
  (:require clojure.java.io))

(defn- get-git-log-maps []
  "Parses /tmp for Git push .edn files, reads them into Clojure maps,
   then removes the files from the file system. It returns a list of the
   maps."
  (binding [*read-eval* false]
    (let [log-files (->> (file-seq (clojure.java.io/file "/tmp"))
                         (map str)
                         (filter #(re-matches #".*irc.*git.*\.edn$" %)))
          log-maps (->> log-files
                        (map slurp)
                        (map read-string)
                        (doall))]
      (doseq [f log-files] (clojure.java.io/delete-file f))
      log-maps)))

(defn run [connection]
  (let [git-log-maps (get-git-log-maps)
        messages (map #(str "PRIVMSG " (connection :channel)
                            " :" (count (% :commits)) " commits pushed to "
                            (% :repo) " on branch " (% :branch) ".\n")
                      git-log-maps)]
    messages))

(defn parse [connection line])
