(defproject pgbot "0.5.1"
  :description "An IRC bot, written in Clojure."
  :license {:name "MIT"
            :url "http://choosealicense.com/licenses/mit/"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]
                 [compojure "1.1.6"]
                 [ring/ring-jetty-adapter "1.1.8"]
                 [org.clojure/core.typed "0.2.19"]
                 [com.datomic/datomic-free "0.9.4324"]
                 [com.taoensso/timbre "2.7.1"]]
  :repositories {"sonatype-oss-public"
                 "https://oss.sonatype.org/content/groups/public/"}
  :main pgbot.core
  :profiles {:dev {:source-paths ["dev"]
                   :repl-options {:init-ns user}
                   :dependencies [[org.clojure/tools.namespace "0.2.3"]]}})
