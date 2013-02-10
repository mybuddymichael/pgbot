(ns pgbot.core
  "A simple IRC bot."
  (:require overtone.at-at))

(def ^:private plugins
  #{'pgbot.plugin.help})

(def ^:private thread-pool
  "Returns the app's thread pool for interval-based code execution."
  (overtone.at-at/mk-pool))

(defn- create-connection
  "Opens a connection to a server. Returns a map containing information
   about the connection."
  [host port nick channel]
  (let [socket (java.net.Socket. host (Integer. port))]
    {:socket socket
     :in (clojure.java.io/reader socket)
     :out (clojure.java.io/writer socket)
     :nick nick
     :channel channel}))

(defn- send-message
  "Sends a message through a connection's writer. This takes multiple
   string arguments and will join them with spaces in-between, or, if
   message is sequential, it prints out each string in the list."
  [connection message & messages]
  (binding [*out* (connection :out)]
    (if (sequential? message)
      (doseq [m message] (println m))
      (println (clojure.string/join " " (cons message messages))))))

(defn- register-connection
  "Sends a 'handshake' message to register the connection."
  [connection]
  (let [nick (connection :nick)]
    (send-message connection "NICK" nick)
    (send-message connection "USER" nick "i *" nick)))

(defn- read-line-from-connection
  "Reads a single line from the connection. Returns nil if the socket is
   closed."
  [connection]
  (try (.readLine (connection :in))
    (catch java.net.SocketException _ nil)))

(defn- ping-pong
  "Returns a PONG string if the line is a PING."
  [line]
  (when-let [[_ server] (re-find #"^PING :(.+)" line)]
    (str "PONG :" server)))

(defn connect
  "Entry point for operating the bot. This creates a connection, does
   the dance to ensure that it stays open, and begins listening for
   messages in a new thread. It returns the connection map."
  [host port nick channel]
  (let [connection (create-connection host port nick channel)]
    (future
      (register-connection connection)
      (send-message connection "JOIN" channel)
      (overtone.at-at/every
        10000
        #(doseq [p @pgbot.plugin/plugins]
           (require `~p)
           (when-let [message ((ns-resolve p 'run) connection)]
             (send-message connection message)))
        thread-pool
        :initial-delay 30000)
      (loop [line (read-line-from-connection connection)]
        (when line
          (println line)
          (when-let [message (ping-pong line)]
            (send-message connection message)
            (println message))
          (when (re-find (re-pattern (str ":" (connection :nick))) line)
            (doseq [p @pgbot.plugin/plugins]
              (require `~p)
              (when-let [message ((ns-resolve p 'parse) connection line)]
                (send-message connection message)
                (println message))))
          (recur (read-line-from-connection connection)))))
    connection))
