(ns pgbot.commit-server
  (:require pgbot.events
            [clojure.core.async :refer [chan go >!]]
            [compojure.core :refer [routes POST]]
            [compojure.handler :refer [api]]
            [ring.adapter.jetty :refer [run-jetty]]))

(defn create
  "Creates a stopped Jetty Server and returns a map containing the
   Server and its output channel."
  [listening-port irc-channel]
  (let [out (chan)
        server (run-jetty
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
    {:server server
     :out out}))

(defn start
  "Runs side effects to start the Jetty Server that listens for git
   commits. Returns the started server."
  [{:keys [server] :as commit-server}]
  (.start server)
  commit-server)

(defn stop
  "Runs side effects to stop the commit server."
  [{:keys [server] :as commit-server}]
  (.stop server)
  commit-server)
