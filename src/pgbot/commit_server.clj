(ns pgbot.commit-server
  (:require (pgbot [process :refer [PProcess]])
            [clojure.core.async :refer [chan go put!]]
            [compojure.core :refer [routes POST]]
            [compojure.handler :refer [api]]
            [ring.adapter.jetty :refer [run-jetty]]))

(defrecord CommitServer [server out])

(extend-type CommitServer
  PProcess
  (start [{:keys [server] :as commit-server}]
    (.start server)
    commit-server)
  (stop [{:keys [server] :as commit-server}]
    (.stop server)
    commit-server))

(defn ->CommitServer
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
                            (put! out {:type "PRIVMSG"
                                       :destination irc-channel
                                       :content message}))
                          {:body nil})))
                 {:port listening-port :join? false})]
    (.stop server)
    (CommitServer. server out)))
