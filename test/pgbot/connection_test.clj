(ns pgbot.connection-test
  (:require [clojure.test :use [is]]
            [pgbot.test-helpers :use [deftest* deftest-*]]
            pgbot.connection))

(defonce connection
  (pgbot.connection/create "irc.freenode.net" 6667 "pgbot" "##pgbottest"))

(.close (connection :socket))

(deftest* "create returns a socket with socket, in, out, nick, and channel"
  (is (= (map (fn [x] true) (range 5))
         (map #(contains? connection %) [:socket :in :out :nick :channel]))))

(deftest* "connection has a socket"
  (is (= java.net.Socket
         (class (connection :socket)))))

(deftest* "connection has a reader"
  (is (= java.io.BufferedReader
         (class (connection :in)))))

(deftest* "connection has a writer"
  (is (= java.io.BufferedWriter
         (class (connection :out)))))

(deftest* "connection has a nick"
  (is (= "pgbot"
         (connection :nick))))

(deftest* "connection has a channel"
  (is (= "##pgbottest"
         (connection :channel))))

(deftest* "send-message writes the message to connection :out"
  (let [connection {:out (java.io.StringWriter.)}]
    (pgbot.connection/send-message connection {:prefix "m@m.net"
                                               :type "PRIVMSG"
                                               :destination "##pgbottest"
                                               :content "Hi, buddy."})
    (is (= ":m@m.net PRIVMSG ##pgbottest :Hi, buddy.\n"
           (str (connection :out))))))

(deftest* "register-connection triggers the appropriate handshake messages"
  (let [connection {:out (java.io.StringWriter.)
                    :nick "pgbot"
                    :events {:outgoing [pgbot.connection/send-message]}}]
    (pgbot.connection/register connection)
    (is (= "NICK pgbot\nUSER pgbot i * pgbot\n"
           (str (connection :out))))))

(deftest* "get-message gets a single message map"
  (let [connection
        (pgbot.connection/create "irc.freenode.net" 6667 "pgbot"
                                 "##pgbottest")]
    (is (map? (pgbot.connection/get-line connection)))
    (.close (connection :socket))))

(deftest* "get-message returns nil if the socket is closed"
  (let [connection
        (pgbot.connection/create "irc.freenode.net" 6667 "pgbot"
                                 "##pgbottest")]
    (.close (connection :socket))
    (is (nil? (pgbot.connection/get-line connection)))))

(deftest* "ping-pong triggers a pong message when pinged"
  (let [connection {:out (java.io.StringWriter.)
                    :events {:outgoing [pgbot.connection/send-message]}}]
    (pgbot.connection/ping-pong connection {:type "PING" :content "server-name"})
    (is (= "PONG :server-name\n"
           (str (connection :out))))))
