(ns pgbot.connection
  (:require (pgbot [messages :refer [parse compose]])
            [clojure.core.async :refer [chan thread put! alts!! close!]]))

(defn- get-line
  "Grabs a single line from the connection, parsing it into a message
   map, or returning nil if the socket is closed."
  [connection]
  (try (let [line (.readLine (connection :reader))]
         (if line (parse line) nil))
    (catch java.io.IOException _ nil)))

(defn- send-message
  "Sends one or more messages through a connection's writer."
  [connection & messages]
  (binding [*out* (connection :writer)]
    (doseq [message messages]
      (println (compose message)))))

(defn create
  "Creates and returns a map for holding the physical connection to the
   IRC server."
  [host port nick channel in-chans out-chans out-listeners]
  {:socket nil
   :reader nil
   :writer nil
   :host host
   :port port
   :nick nick
   :channel channel
   :in-loop nil
   :out-loop nil
   :in-chans in-chans
   :out-chans out-chans
   :out-listeners out-listeners
   :stop (chan)})

(defn start
  "Runs side effects to open a connection to an IRC server. If it cannot
   establish a connection it will keep trying until it succeeds. It
   starts placing incoming messages into the in channel and taking
   outgoing message off the out channel."
  [{:keys [host port nick channel in-chans out-chans out-listeners stop]
    :as connection}]
  (let [open-socket
        (fn [host port]
          (or (try (java.net.Socket. host port)
                   (catch java.net.UnknownHostException _ nil))
              (recur host port)))
        socket (open-socket host port)
        _ (.setSoTimeout socket 300000)
        connection (assoc connection
                          :socket socket
                          :reader (clojure.java.io/reader socket)
                          :writer (clojure.java.io/writer socket))]
    (send-message connection
                  {:type "NICK" :destination nick}
                  {:type "USER" :destination (str nick " i * " nick)})
    (send-message connection
                  {:type "JOIN" :destination channel})
    (assoc connection
           :in-loop
           (thread
             (loop [line (get-line connection)]
               (when line
                 (doseq [c in-chans] (put! c line))
                 (recur (get-line connection)))))
           :out-loop
           (thread
             (let [alts-fn #(alts!! (flatten [stop out-chans]) :priority true)]
               (loop [[message chan] (alts-fn)]
                 (when (not= chan stop)
                   (send-message connection message)
                   (doseq [c out-listeners] (put! c message))
                   (recur (alts-fn)))))))))

(defn stop [{:keys [socket reader writer stop] :as connection}]
  (doseq [s [socket reader writer]]
    (.close s))
  (close! stop)
  connection)
