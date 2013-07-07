(ns pgbot.application
  (:require (pgbot connection
                   commit-server
                   ping-pong
                   dispatcher
                   logger)
            [clojure.core.async :refer [chan]]))

(defn create
  "Creates and returns a new instance of pgbot."
  [{:keys [host port nick channel commit-server-port]}]
  (let [port (Integer. port)
        commit-server-port (Integer. commit-server-port)
        {:keys [out channel] :as connection} (pgbot.connection/create
                                               host port nick channel)
        ping-pong (pgbot.ping-pong/create out)
        commit-server (pgbot.commit-server/create
                        commit-server-port out channel)]
    {:connection connection
     :ping-pong ping-pong
     :commit-server commit-server
     :dispatcher (pgbot.dispatcher/create [(ping-pong :in)])}))

(defn create-dev
  "Creates and returns a new pgbot instance suitable for development."
  [& _]
  (create {:host "irc.freenode.net"
           :port 6667
           :nick "pgbottest"
           :channel "##pgbottest"
           :commit-server-port 8080}))

(defn start
  "Runs various side effects to start up pgbot. Returns the started
   application."
  [{:keys [connection commit-server ping-pong dispatcher] :as application}]
  (let [connection
        (-> connection
            pgbot.logger/start
            pgbot.connection/start)]
    (assoc application
           :connection connection
           :commit-server (pgbot.commit-server/start commit-server)
           :ping-pong (pgbot.ping-pong/stop ping-pong)
           :dispatcher (pgbot.dispatcher/start dispatcher))))

(defn stop
  "Runs various side effects to shut down pgbot. Returns the stopped
   application."
  [{:keys [connection commit-server ping-pong dispatcher] :as application}]
  (assoc application
         :commit-server (pgbot.commit-server/stop commit-server)
         :connection (pgbot.connection/stop connection)
         :ping-pong (pgbot.ping-pong/stop ping-pong)
         :dispater (pgbot.dispatcher/stop dispatcher)))
