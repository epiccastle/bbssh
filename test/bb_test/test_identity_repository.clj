(ns bb-test.test-identity-repository
  (:require [pod.epiccastle.bbssh.agent :as agent]
            [pod.epiccastle.bbssh.session :as session]
            [pod.epiccastle.bbssh.channel-exec :as channel-exec]
            [pod.epiccastle.bbssh.input-stream :as input-stream]
            [pod.epiccastle.bbssh.output-stream :as output-stream]
            [pod.epiccastle.bbssh.key-pair :as key-pair]
            [pod.epiccastle.bbssh.identity :as identity]
            [pod.epiccastle.bbssh.identity-repository :as identity-repository]
            [pod.epiccastle.bbssh.cleaner :as cleaner]
            [pod.epiccastle.bbssh.utils :as utils]
            [bb-test.docker :as docker]
            [bb-test.keys :as keys]
            [clojure.test :refer [is deftest]]
            [clojure.string :as string]))

(defn streams-for-out []
  (let [os (output-stream/new)
        is (input-stream/new os 1024)]
    [os is]))

(defn create-key-pair [agent key-id]
  (spit "/tmp/bbssh-test-key" (get-in keys/keys [key-id :private]))
  (spit "/tmp/bbssh-test-key.pub" (get-in keys/keys [key-id :public]))
  (key-pair/load agent "/tmp/bbssh-test-key" "/tmp/bbssh-test-key.pub"))

(deftest identity-repository
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})

  (docker/exec "mkdir /root/.ssh")
  (docker/put-file
   (get-in keys/keys [:rsa-nopassphrase :public])
   "/root/.ssh/authorized_keys")

  (let [{:keys [username hostname port password]}
        {:username "root"
         :password "root-access-please"
         :hostname "localhost"
         :port 9876}
        ]
    (let [agent (agent/new)
          kp (create-key-pair agent :rsa-nopassphrase)
          idn (identity/new
               {:set-passphrase
                (fn [passphrase]
                  (prn :set-passphrase passphrase)
                  true)
                :get-public-key-blob
                (fn []
                  (prn :get-public-key-blob)
                  (key-pair/get-public-key-blob kp))
                :get-signature
                (fn
                  ([data]
                   (prn :get-signature data)
                   (key-pair/get-signature kp data))
                  ([data algo]
                   (prn :get-signature data algo)
                   (key-pair/get-signature kp data algo)))
                :decrypt
                (fn []
                  (prn :decrypt))
                :get-alg-name
                (fn []
                  (prn :get-alg-name)
                  ;; this should come from public key start
                  "ssh-rsa"
                  )
                :get-name
                (fn []
                  (prn :get-name))
                :is-encrypted
                (fn []
                  (prn :is-encrypted)
                  false)
                :clear
                (fn []
                  (prn :clear))})
          id-repo (identity-repository/new
                   {:get-name
                    (fn [] (prn :get-name))
                    :get-status
                    (fn [] (prn :get-status))
                    :get-identities
                    (fn []
                      (prn :get-identities)
                      [idn])
                    :add
                    (fn [data] (prn :add data))
                    :remove
                    (fn [data] (prn :remove data))
                    :remove-all
                    (fn [] (prn :remove-all))})
          _ (agent/set-identity-repository agent id-repo)
          session (agent/get-session agent username hostname port)]
      (is (= id-repo (agent/get-identity-repository agent)))

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

  (docker/cleanup)
  )
