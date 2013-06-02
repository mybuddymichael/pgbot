(ns pgbot.git-listener
  (:require [compojure.core :use [defroutes POST]]
            [compojure.handler :use [api]]
            [ring.adapter.jetty :use [run-jetty]]))

(def connection (atom nil))

(defroutes app-routes
  (POST "/" [user-name commit-message branch sha]
        {:body nil}))

(def application (api app-routes))

(defn start-server [] (run-jetty application {:port 8080 :join? false}))
