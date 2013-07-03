(ns pgbot.commit-server
  (:require pgbot.events
            [clojure.core.async :refer [go >!]]
            [compojure.core :refer [routes POST]]
            [compojure.handler :refer [api]]
            [ring.adapter.jetty :refer [run-jetty]]))

(defn create
  "Creates and returns a stopped Jetty Server instance."
  [listening-port out irc-channel]
  (let [server (run-jetty
                 (api (routes
                        (POST "/" [user-name commit-message repo branch sha]
                          (let [message
                                (str user-name " in " repo "/" branch ": \""
                                     commit-message"\" (" sha ")")]
                            (go
                              (>! out {:type "PRIVMSG"
                                       :destination irc-channel
                                       :content message})))
                          {:body nil})))
                 {:port listening-port :join false})]
    (.stop server)
    server))

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
