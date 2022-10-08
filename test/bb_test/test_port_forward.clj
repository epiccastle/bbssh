(ns bb-test.test-port-forward
  (:require [pod.epiccastle.bbssh.core :as bbssh]
            [pod.epiccastle.bbssh.session :as session]
            [babashka.process :as process]
            [babashka.curl :as curl]
            [bb-test.docker :as docker]
            [clojure.test :refer [is deftest]]
            [clojure.java.io :as io]
            [clj-commons.digest :as digest])
  (:import [java.io File BufferedInputStream]
           [java.net Socket]))

(defn read-stream-line [stream]
  (loop [out ""]
    (let [c (.read stream)]
      (case c
        -1 out
        10 (str out (char c))
        (recur (str out (char c)))))))

(deftest test-port-forward-local
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})

  (let [session
        (bbssh/ssh "localhost" {:port 9876
                                :username "root"
                                :password "root-access-please"
                                :strict-host-key-checking false})]
    (is (empty? (session/get-port-forwarding-local session)))
    (session/set-port-forwarding-local session
                                       {:local-port 56780
                                        :remote-host "localhost"
                                        :remote-port 22
                                        :connection-timeout 30000
                                        })
    (is (= (session/get-port-forwarding-local session)
           [{:local-port 56780
             :remote-host "localhost"
             :remote-port 22}]))
    (let [sock (Socket. "localhost" 56780)
          output (.getOutputStream sock)
          input (.getInputStream sock)]
      (is (= (read-stream-line input)
             "SSH-2.0-OpenSSH_9.0\r\n"))
      (.write output (.getBytes "invalid\n"))
      (is (= (read-stream-line input)
             "Invalid SSH identification string.\r\n"))
      (.close sock))
    (session/delete-port-forwarding-local session {:local-port 56780})
    (is (empty? (session/get-port-forwarding-local session)))
    (is (thrown? java.net.ConnectException (Socket. "localhost" 56780))))

  (docker/cleanup))

(deftest test-port-forward-remote
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})

  (let [session
        (bbssh/ssh "localhost" {:port 9876
                                :username "root"
                                :password "root-access-please"
                                :strict-host-key-checking false})]
    (is (empty? (session/get-port-forwarding-remote session)))
    (session/set-port-forwarding-remote session
                                        {:remote-port 56780
                                         :local-host "epiccastle.io"
                                         :local-port 80
                                         :connection-timeout 30000
                                         })
    (is (= (session/get-port-forwarding-remote session)
           [{:remote-port 56780
             :local-host "epiccastle.io"
             :local-port 80}]))
    (is (-> (docker/exec "curl http://localhost:56780")
            (.contains "301 Moved Permanently")))
    (session/delete-port-forwarding-remote session {:remote-port 56780})
    (is (empty? (session/get-port-forwarding-remote session)))
    (is (-> (docker/exec! "curl http://localhost:56780")
            :err
            (.contains "Connection refused"))))

  (docker/cleanup))
