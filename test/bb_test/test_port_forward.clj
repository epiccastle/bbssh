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
            (.contains "Failed to connect to localhost port 56780"))))

  (docker/cleanup))

(deftest test-port-forward-local-core-ssh
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})

  (let [session
        (bbssh/ssh
         "localhost"
         {:port 9876
          :username "root"
          :password "root-access-please"
          :strict-host-key-checking false
          :port-forward-local [{:local-port 56780
                                :remote-host "localhost"
                                :remote-port 22}]
          })]
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

(deftest test-port-forward-remote-core-ssh
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})

  (let [session
        (bbssh/ssh
         "localhost"
         {:port 9876
          :username "root"
          :password "root-access-please"
          :strict-host-key-checking false
          :port-forward-remote [{:remote-port 56780
                                 :local-host "epiccastle.io"
                                 :local-port 80}]})]
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
            (.contains "Failed to connect to localhost port 56780"))))

    (docker/cleanup))

;;
;; Unix domain socket on the remote
;;
(defn stop-unix-socket-server
  []
  (docker/exec "sh -c 'pkill nc || true'"))

(defn start-unix-socket-server
  "Start a server on the remote that talks over a unix domain socket"
  []
  (stop-unix-socket-server)
  (let [;; Nohup is required so the docker exec could exit but keep the cmd
        ;; running in the background.
        cmd "sh -c 'nohup echo \"remote to local\" | /usr/bin/nc -U -l /var/run/test-socket > /tmp/unix-socket.log &'"]
    (docker/exec cmd)))

(deftest test-port-forward-local-unix-domain
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})
  (start-unix-socket-server)

  (let [session
        (bbssh/ssh "localhost" {:port 9876
                                :username "root"
                                :password "root-access-please"
                                :strict-host-key-checking false})]
    (is (empty? (session/get-port-forwarding-local session)))
    (session/set-port-forwarding-local session
                                       {:local-port 56780
                                        :remote-unix-socket "/var/run/test-socket"
                                        :connection-timeout 30000
                                        })
    (is (= (session/get-port-forwarding-local session)
           [{:local-port 56780}]))
    (let [sock (Socket. "localhost" 56780)
          output (.getOutputStream sock)
          input (.getInputStream sock)]
      (is (= (read-stream-line input)
             "remote to local\n"))
      (.write output (.getBytes "local to remote\n"))
      (.close sock))
    (is (= (docker/exec "cat /tmp/unix-socket.log")
           "local to remote\n"))

    (stop-unix-socket-server)
    (session/delete-port-forwarding-local session {:local-port 56780})
    (is (empty? (session/get-port-forwarding-local session)))
    (is (thrown? java.net.ConnectException (Socket. "localhost" 56780))))

  (docker/cleanup))

(deftest test-port-forward-local-unix-domain-core-ssh
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})
  (start-unix-socket-server)

  (let [session
        (bbssh/ssh
         "localhost"
         {:port 9876
          :username "root"
          :password "root-access-please"
          :strict-host-key-checking false
          :port-forward-local [{:local-port 56780
                                :remote-unix-socket "/var/run/test-socket"}]
          })]
    (is (= (session/get-port-forwarding-local session)
           [{:local-port 56780}]))
    (let [sock (Socket. "localhost" 56780)
          output (.getOutputStream sock)
          input (.getInputStream sock)]
      (is (= (read-stream-line input)
             "remote to local\n"))
      (.write output (.getBytes "local to remote\n"))
      (.close sock))
    (is (= (docker/exec "cat /tmp/unix-socket.log")
           "local to remote\n"))

    (stop-unix-socket-server)
    (session/delete-port-forwarding-local session {:local-port 56780})
    (is (empty? (session/get-port-forwarding-local session)))
    (is (thrown? java.net.ConnectException (Socket. "localhost" 56780))))

  (docker/cleanup))
