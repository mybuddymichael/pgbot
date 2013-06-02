(ns pgbot.git-listener
  (:require [pgbot.events :use [trigger-event]]
            [compojure.core :use [defroutes POST]]
            [compojure.handler :use [api]]
            [ring.adapter.jetty :use [run-jetty]]))

(def connection (atom nil))

(defroutes app-routes
  (POST "/" [user-name commit-message branch sha]
        (let [message
              (str user-name " on " branch ": \""
                   commit-message"\" (" sha ")")]
          (trigger-event @connection :outgoing
                         {:type "PRIVMSG"
                          :destination (:channel @connection)
                          :content message}))
        {:body nil}))

(def application (api app-routes))

(defn start-server [] (run-jetty application {:port 8080 :join? false}))
