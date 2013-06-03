(defproject pgbot "0.2.0"
  :description "A simple IRC bot, written in Clojure."
  :license {:name "LGPLv3"
            :url "http://www.gnu.org/licenses/lgpl.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.5"]
                 [ring/ring-jetty-adapter "1.1.8"]
                 [org.clojure/tools.namespace "0.2.3"]]
  :test-paths ["test"]
  :main pgbot.core)
