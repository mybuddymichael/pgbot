(defproject pgbot "0.1.0"
  :description "A simple IRC bot, written in Clojure."
  :license {:name "The MIT License"
            :url "http://www.opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [overtone/at-at "1.1.1"]]
  :test-paths ["test" "src/pgbot/plugin"]
  :main pgbot.core)
