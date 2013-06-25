(ns pgbot.commit-server
  (:require pgbot.events
            [compojure.core :refer [routes POST]]
            [compojure.handler :refer [api]]
            [ring.adapter.jetty :refer [run-jetty]]))

(defn start-server [connection & {:keys [port] :or {port 8080}}]
  (let [app-routes
        (routes
          (POST "/" [user-name commit-message repo branch sha]
                (let [message
                      (str user-name " in " repo "/" branch ": \""
                           commit-message"\" (" sha ")")]
                  (pgbot.events/trigger
                    connection
                    :outgoing
                    {:type "PRIVMSG"
                     :destination (:channel connection)
                     :content message}))
                {:body nil}))
        handler (api app-routes)]
    (run-jetty handler {:port (Integer. port) :join? false})))
