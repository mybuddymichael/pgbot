(ns pgbot.core-test
  (:require [clojure.test :refer [is]]
            pgbot.core))

(defmacro ^:private deftest*
  "Define a test using deftest, but with a string as the name instead of
   a symbol."
  [name-string & body]
  (let [name-symbol
        (-> name-string
            clojure.string/lower-case
            (clojure.string/replace #"\W" "-")
            (clojure.string/replace #"-+" "-")
            (clojure.string/replace #"-$" "")
            symbol)]
  `(clojure.test/deftest ~name-symbol ~@body)))

(defonce connection
  (#'pgbot.core/create-connection "irc.freenode.net" 6667 "pgbot"
                                  "##pgbottest"))
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

(deftest* "plugins is a seq of symbols"
  (is (seq? @#'pgbot.core/plugins))
  (is (= clojure.lang.Symbol
         (class (first @#'pgbot.core/plugins)))))

(deftest* "send-message writes the message to connection :out"
  (let [connection {:out (java.io.StringWriter.)}]
    (#'pgbot.core/send-message connection "Test string.")
    (is (= "Test string.\n"
           (str (connection :out))))))

(deftest* "send-message concatenates multiple args with spaces"
  (let [connection {:out (java.io.StringWriter.)}]
    (#'pgbot.core/send-message connection "This" "and" "that.")
    (is (= "This and that.\n"
           (str (connection :out))))))

(deftest* "register-connection sends the appropriate handshake message"
  (let [connection {:out (java.io.StringWriter.)
                    :nick "pgbot"}]
    (#'pgbot.core/register-connection connection)
    (is (= "NICK pgbot\nUSER pgbot i * pgbot\n"
           (str (connection :out))))))

(deftest* "read-line-from-connection gets a single line"
  (let [connection
        (#'pgbot.core/create-connection "irc.freenode.net" 6667 "pgbot"
                                        "##pgbottest")]
    (is (string? (#'pgbot.core/read-line-from-connection connection)))
    (.close (connection :socket))))

(deftest* "read-line-from-connection returns nil if the socket is closed"
  (let [connection
        (#'pgbot.core/create-connection "irc.freenode.net" 6667 "pgbot"
                                        "##pgbottest")]
    (.close (connection :socket))
    (is (nil? (#'pgbot.core/read-line-from-connection connection)))))

(deftest parse-line-pongs-the-server
  (is (= "PONG :server-name"
         (#'pgbot.core/parse-line "PING :server-name"))))
