(ns bb-test.core
  (:require [babashka.pods :as pods]
            [babashka.process :as process]
            [babashka.wait :as wait]
            [clojure.string :as string]
            [bb-test.docker :as docker]
            )
  (:import [java.lang.ref WeakReference]))

(def class-path
  (-> "clj -Spath -m bbssh.core"
      process/sh
      :out
      string/trim))

;; run pod process under java in dev
(pods/load-pod ["java"
                "-Djava.library.path=resources"
                "-cp"
                class-path
                "clojure.main"
                "-m"
                "bbssh.core"] {:transport :socket})

;; run pod process from native-image to test
#_(pods/load-pod "./bbssh" {:transport :socket})

(require '[pod.epiccastle.bbssh.agent :as agent]
         '[pod.epiccastle.bbssh.session :as session]
         '[pod.epiccastle.bbssh.channel-exec :as channel-exec]
         '[pod.epiccastle.bbssh.input-stream :as input-stream]
         '[pod.epiccastle.bbssh.output-stream :as output-stream]
         '[pod.epiccastle.bbssh.impl.cleaner :as cleaner])

(docker/cleanup)
(docker/build {:root-password "root-access-please"})
(docker/start {:ssh-port 9876})

(defn streams-for-out []
  (let [os (output-stream/new)
        is (input-stream/new os 1024)]
    [os is]))

(defn test-exec-basic [{:keys [username hostname port password]}]
  (let [agent (agent/new)
        session (agent/get-session agent username hostname port)]
    (session/set-password session password)
    (session/set-config session :strict-host-key-checking false)
    (session/connect session)
    (assert (session/connected? session))
    (let [channel (session/open-channel session "exec")
          is (input-stream/new)
          [out-stream out-in] (streams-for-out)
          [err-stream err-in] (streams-for-out)
          ]
      (channel-exec/set-command channel "id")
      (channel-exec/set-input-stream channel is false)
      (input-stream/close is)

      (channel-exec/set-output-stream channel out-stream)
      (channel-exec/set-error-stream channel err-stream)

      (channel-exec/connect channel)
      (let [buff (byte-array 1024)
            num (input-stream/read out-in buff 0 1024)]
        (assert
         (-> (java.util.Arrays/copyOfRange buff 0 num)
             (String. "UTF-8")
             (string/starts-with? "uid=0(root) gid=0(root)")))))))

(test-exec-basic {:username "root"
                  :password "root-access-please"
                  :hostname "localhost"
                  :port 9876})

(docker/cleanup)
