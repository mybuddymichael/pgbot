(ns pgbot.commit-server
  (:require (pgbot [lifecycle :refer [Lifecycle]]
                   [messages :as messages])
            [clojure.core.async :as async]
            [compojure.core :refer [routes POST]]
            [compojure.handler :refer [api]]
            [ring.adapter.jetty :refer [run-jetty]]
            [taoensso.timbre :refer [info]]))

(defrecord CommitServer [server out]
  Lifecycle
  (start [commit-server]
    (.start server)
    (info "Commit server started.")
    commit-server)
  (stop [commit-server]
    (.stop server)
    (async/close! out)
    (info "Commit server stopped.")
    commit-server))

(defn create
  "Creates a stopped Jetty Server and returns it in a CommitServer."
  [buffer-size listening-port irc-channel]
  (let [out (async/chan buffer-size)
        server (run-jetty
                 (api (routes
                        (POST "/" [user-name commit-message repo branch sha]
                          (let [message
                                (str user-name " in " repo "/" branch ": \""
                                     commit-message"\" (" sha ")")]
                            (async/put! out (messages/privmsg irc-channel
                                                              message))
                          {:body nil}))))
                 {:port listening-port :join? false})]
    (.stop server)
    (CommitServer. server out)))
