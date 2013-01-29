(defproject pgbot "0.1.1"
  :description "A simple IRC bot, written in Clojure."
  :license {:name "LGPLv3"
            :url "http://www.gnu.org/licenses/lgpl.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [overtone/at-at "1.1.1"]]
  :test-paths ["test" "src/pgbot/plugin"]
  :main pgbot.core)
