(ns pgbot.core
  "A simple IRC bot.")

(def ^:private plugins
  "Returns seq of plugin namespace symbols."
  (->> (file-seq (clojure.java.io/file "src/pgbot/plugin"))
       (map str)
       (filter #(re-matches #".*\.clj" %))
       (map (fn [string]
              (let [[_ namespace-string]
                    (re-matches #".*plugin/([A-Za-z_]+)\.clj$" string)]
                (symbol (str "pgbot.plugin." namespace-string)))))))

(defn- create-connection
  "Open a connection to a server. Returns a map containing information
   about the connection."
  [host port nick channel]
  (let [socket (java.net.Socket. host (Integer. port))]
    {:socket socket
     :in (clojure.java.io/reader socket)
     :out (clojure.java.io/writer socket)
     :nick nick
     :channel channel}))

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

(defn- read-line-from-connection
  "Read a single line from the connection. Returns nil if the socket is
   closed."
  [connection]
  (try (.readLine (connection :in))
    (catch java.net.SocketException _ nil)))

(defn- parse-line
  "Check if a line matches our rules. Returns a string or returns nil."
  [line]
  (when-let [[_ server] (re-find #"^PING :(.+)" line)]
    (str "PONG :" server)))

(defn connect
  "Entry point for operating the bot. This creates a connection, does
   the dance to ensure that it stays open, and begins listening for
   messages in a new thread.

   It returns the connection map."
  [host port nick channel]
  (let [connection
        (create-connection "irc.freenode.net" 6667 "pgbot" "##pgbottest")]
    (register-connection connection)
    (send-message connection "JOIN" channel)
    (future
      (loop [line (read-line-from-connection connection)]
        (when line
          (println line)
          (when-let [message (parse-line line)]
            (send-message connection message))
          (recur (read-line-from-connection connection)))))
    connection))
