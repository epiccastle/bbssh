(ns bb-test.test-password-exec
  (:require [pod.epiccastle.bbssh.agent :as agent]
            [pod.epiccastle.bbssh.session :as session]
            [pod.epiccastle.bbssh.channel-exec :as channel-exec]
            [pod.epiccastle.bbssh.input-stream :as input-stream]
            [pod.epiccastle.bbssh.output-stream :as output-stream]
            [bb-test.docker :as docker]
            [clojure.test :refer [is deftest]]
            [clojure.string :as string]))

(defn streams-for-out []
  (let [os (output-stream/new)
        is (input-stream/new os 1024)]
    [os is]))

(deftest password-exec
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})

  (let [{:keys [username hostname port password]}
        {:username "root"
         :password "root-access-please"
         :hostname "localhost"
         :port 9876}
        ]
    (let [agent (agent/new)
          session (agent/get-session agent username hostname port)]
      (session/set-password session password)
      (session/set-config session :strict-host-key-checking false)
      (session/connect session)
      (assert (session/connected? session))
      (let [channel (session/open-channel session "exec")
            input-stream (input-stream/new)
            [out-stream out-in] (streams-for-out)
            [err-stream err-in] (streams-for-out)
            ]
        (channel-exec/set-command channel "id")
        (channel-exec/set-input-stream channel input-stream false)
        (input-stream/close input-stream)

        (channel-exec/set-output-stream channel out-stream)
        (channel-exec/set-error-stream channel err-stream)

        (channel-exec/connect channel)
        (let [buff (byte-array 1024)
              num (input-stream/read out-in buff 0 1024)
              result (-> (java.util.Arrays/copyOfRange buff 0 num)
                         (String. "UTF-8")
                         )]
          (is (string/starts-with? result "uid=0(root) gid=0(root)"))))))

  (docker/cleanup))
