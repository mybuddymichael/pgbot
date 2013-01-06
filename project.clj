(defproject pgbot "0.0.1"
  :description "A simple IRC bot, written in Clojure."
  :license {:name "The MIT License"
            :url "http://www.opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.4.0"]]
  :test-paths ["test" "src/pgbot/plugin"]
  :main pgbot.core)
