(ns pgbot.core
  "A simple IRC bot.")

(defn- create-connection
  "Create a connection and get a map containing information about it."
  [host port nick]
  (let [socket (java.net.Socket. host (Integer. port))]
    {:socket socket
     :in (clojure.java.io/reader socket)
     :out (clojure.java.io/writer socket)
     :nick nick}))

(defn- send-message
  "Send a message through a connection's writer. This takes multiple
  string arguments and will join them with spaces in-between."
  [connection message & messages]
  (binding [*out* (connection :out)]
    (println (clojure.string/join " " (cons message messages)))))

(defn- register-connection
  "Send a 'handshake' message to register the connection."
  [connection]
  (let [nick (connection :nick)]
    (send-message connection "NICK" nick)
    (send-message connection "USER" nick "i *" nick)))
