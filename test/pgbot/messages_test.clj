(ns pgbot.messages-test
  (:require [clojure.test :use [is]]
            [pgbot.test-helpers :use [deftest* deftest-*]]
            pgbot.messages))

(deftest* "parse returns a map with message parts"
  (is (= {:prefix "m@m.net"
          :type "PRIVMSG"
          :destination "##pgbottest"
          :content "Hi."}
         (pgbot.messages/parse ":m@m.net PRIVMSG ##pgbottest :Hi."))))

(deftest* "compose reconstructs a message string"
  (is (= "PING :hemingway.servers.net"
         (pgbot.messages/compose {:prefix nil
                                  :type "PING"
                                  :destination nil
                                  :content "hemingway.servers.net"})))
  (is (= ":m@m.net PRIVMSG ##pgbottest :Hi."
         (pgbot.messages/compose {:prefix "m@m.net"
                                  :type "PRIVMSG"
                                  :destination "##pgbottest"
                                  :content "Hi."}))))
