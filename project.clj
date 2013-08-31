(defproject pgbot "0.5.0"
  :description "A simple IRC bot, written in Clojure."
  :license {:name "LGPLv3"
            :url "http://www.gnu.org/licenses/lgpl.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [core.async "0.1.0-SNAPSHOT"]
                 [compojure "1.1.5"]
                 [ring/ring-jetty-adapter "1.1.8"]
                 [org.clojure/core.typed "0.2.0"]]
  :repositories {"sonatype-oss-public"
                 "https://oss.sonatype.org/content/groups/public/"}
  :main pgbot.core
  :profiles {:dev {:source-paths ["dev"]
                   :repl-options {:init-ns user}
                   :dependencies [[org.clojure/tools.namespace "0.2.3"]]}})
