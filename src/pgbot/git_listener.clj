(ns pgbot.git-listener
  (:require [pgbot.events :use [trigger-event]]
            [compojure.core :use [defroutes POST]]
            [compojure.handler :use [api]]
            [ring.adapter.jetty :use [run-jetty]]))

(def connection (atom nil))

(defroutes app-routes
  (POST "/" [user-name commit-message repo branch sha]
    (let [message
          (str user-name " in " repo "/" branch ": \""
               commit-message"\" (" sha ")")]
      (trigger-event @connection :outgoing
                     {:type "PRIVMSG"
                      :destination (:channel @connection)
                      :content message}))
    {:body nil}))

(def application (api app-routes))

(defn start-server [& {:keys [port] :or {port 8080}}]
  (run-jetty application {:port (Integer. port) :join? false}))
