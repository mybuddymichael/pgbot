(ns pgbot.web-server
  (:require (pgbot [lifecycle :refer [Lifecycle]]
                   [messages :as messages])
            [clojure.core.async :as async]
            [compojure.core :refer [routes POST]]
            [compojure.handler :refer [api]]
            [ring.adapter.jetty :refer [run-jetty]]
            [taoensso.timbre :refer [info]]))

(defn ^:private ->commit-string
  "Generates a human-readable summary of Git commit information.
   e.g. \"Michael in pgbot/master: \\\"Fix things\\\" (abcd0124)\""
  [user-name commit-message repo branch sha]
  (str user-name " in " repo "/" branch ": \"" commit-message"\" (" sha ")"))

(defrecord WebServer [jetty out]
  Lifecycle
  (start [this]
    (.start jetty)
    (info "Server started.")
    this)
  (stop [this]
    (.stop jetty)
    (async/close! out)
    (info "Server stopped.")
    this))

(defn create
  "Creates a stopped Jetty Server and returns it in a pgbot WebServer."
  [buffer-size listening-port irc-channel]
  (let [out (async/chan buffer-size)
        jetty (run-jetty
                 (api (routes
                        (POST "/" [user-name commit-message repo branch sha]
                          (let [message
                                (str user-name " in " repo "/" branch ": \""
                                     commit-message"\" (" sha ")")]
                            (async/put! out (messages/privmsg irc-channel
                                                              message))
                          {:body nil}))))
                 {:port listening-port :join? false})]
    (.stop jetty)
    (WebServer. jetty out)))
