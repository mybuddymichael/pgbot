(ns pgbot.commit-server
  (:require pgbot.events
            [clojure.core.async :refer [go >!]]
            [compojure.core :refer [routes POST]]
            [compojure.handler :refer [api]]
            [ring.adapter.jetty :refer [run-jetty]]))

(defn- create-handler
  "Generates a Ring handler to post commits to the connection."
  [connection]
  (api (routes
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
           {:body nil}))))

(defn create-and-start
  "Runs side effects to create and start a new Jetty Server that listens
   for git commits. Returns the new Server."
  [connection & {:keys [port] :or {port 8080}}]
  (run-jetty (create-handler connection) {:port (Integer. port) :join? false}))

(defn stop
  "Runs side effects to stop the commit server."
  [server]
  (.stop server)
  server)
