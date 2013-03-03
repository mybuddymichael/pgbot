(ns pgbot.core-test
  (:require [clojure.test :use [is]]
            [pgbot.test-helpers :use [deftest* deftest-*]]
            pgbot.core))

(defonce connection
  (#'pgbot.core/create-connection "irc.freenode.net" 6667 "pgbot"
                                  "##pgbottest"))
(.close (connection :socket))

(deftest-* "create returns a socket with socket, in, out, nick, and channel"
  (is (= (map (fn [x] true) (range 5))
         (map #(contains? connection %) [:socket :in :out :nick :channel]))))

(deftest-* "connection has a socket"
  (is (= java.net.Socket
         (class (connection :socket)))))

(deftest-* "connection has a reader"
  (is (= java.io.BufferedReader
         (class (connection :in)))))

(deftest-* "connection has a writer"
  (is (= java.io.BufferedWriter
         (class (connection :out)))))

(deftest-* "connection has a nick"
  (is (= "pgbot"
         (connection :nick))))

(deftest-* "connection has a channel"
  (is (= "##pgbottest"
         (connection :channel))))

(deftest-* "parse returns a map with message parts"
  (is (= {:prefix "m@m.net"
          :type "PRIVMSG"
          :destination "##pgbottest"
          :content "Hi."}
         (#'pgbot.core/parse ":m@m.net PRIVMSG ##pgbottest :Hi."))))

(deftest-* "compose reconstructs a message string"
  (is (= "PING :hemingway.servers.net"
         (#'pgbot.core/compose
           {:prefix nil
            :type "PING"
            :destination nil
            :content "hemingway.servers.net"})))
  (is (= ":m@m.net PRIVMSG ##pgbottest :Hi."
         (#'pgbot.core/compose
           {:prefix "m@m.net"
            :type "PRIVMSG"
            :destination "##pgbottest"
            :content "Hi."}))))

(deftest-* "send-message writes the message to connection :out"
  (let [connection {:out (java.io.StringWriter.)}]
    (#'pgbot.core/send-message connection {:prefix "m@m.net"
                                           :type "PRIVMSG"
                                           :destination "##pgbottest"
                                           :content "Hi, buddy."})
    (is (= ":m@m.net PRIVMSG ##pgbottest :Hi, buddy.\n"
           (str (connection :out))))))

(deftest-* "register-connection sends the appropriate handshake message"
  (let [connection {:out (java.io.StringWriter.)
                    :nick "pgbot"}]
    (#'pgbot.core/register-connection connection)
    (is (= "NICK pgbot\nUSER pgbot i * pgbot\n"
           (str (connection :out))))))

(deftest-* "read-line-from-connection gets a single line"
  (let [connection
        (#'pgbot.core/create-connection "irc.freenode.net" 6667 "pgbot"
                                        "##pgbottest")]
    (is (string? (#'pgbot.core/read-line-from-connection connection)))
    (.close (connection :socket))))

(deftest-* "read-line-from-connection returns nil if the socket is closed"
  (let [connection
        (#'pgbot.core/create-connection "irc.freenode.net" 6667 "pgbot"
                                        "##pgbottest")]
    (.close (connection :socket))
    (is (nil? (#'pgbot.core/read-line-from-connection connection)))))

(deftest-* "ping-pong returns a pong message when pinged"
  (is (= "PONG :server-name"
         (#'pgbot.core/ping-pong "PING :server-name"))))

(deftest-* "parse returns nil when not being pinged"
  (is (nil? (#'pgbot.core/ping-pong "not a ping message"))))

(deftest-* "events in a map of keywords to fns"
  (is (= [true true true]
         (map fn? (#'pgbot.core/events :incoming))))
  (is (= [true true true]
         (map fn? (#'pgbot.core/events :outgoing)))))
