(ns bb-test.test-identity-repository
  (:require [pod.epiccastle.bbssh.agent :as agent]
            [pod.epiccastle.bbssh.session :as session]
            [pod.epiccastle.bbssh.channel-exec :as channel-exec]
            [pod.epiccastle.bbssh.input-stream :as input-stream]
            [pod.epiccastle.bbssh.output-stream :as output-stream]
            [pod.epiccastle.bbssh.key-pair :as key-pair]
            [pod.epiccastle.bbssh.identity :as identity]
            [pod.epiccastle.bbssh.identity-repository :as identity-repository]
            [bb-test.docker :as docker]
            [bb-test.keys :as keys]
            [clojure.test :refer [is deftest]]
            [clojure.string :as string]))

(defn streams-for-out []
  (let [os (output-stream/new)
        is (input-stream/new os 1024)]
    [os is]))

(def port 9876)

(defn do-connection [session]
  (session/set-config session :strict-host-key-checking false)
  (session/connect session)
  (assert (session/connected? session))
  (let [channel (session/open-channel session "exec")
        input-stream (input-stream/new)
        [out-stream out-in] (streams-for-out)
        [err-stream _err-in] (streams-for-out)
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
      (is (string/starts-with? result "uid=0(root) gid=0(root)"))))
  )

(deftest identity-repository-unencrypted-key
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port port})

  (docker/exec "mkdir /root/.ssh")
  (docker/put-file
   (get-in keys/keys [:rsa-nopassphrase :public])
   "/root/.ssh/authorized_keys")

  (let [agent (agent/new)
        kp (keys/create-key-pair agent :rsa-nopassphrase)
        ident (identity/new
               {:set-passphrase
                (fn [_passphrase]
                  true)
                :get-public-key-blob
                (fn []
                  (key-pair/get-public-key-blob kp))
                :get-signature
                (fn
                  ([data]
                   (key-pair/get-signature kp data))
                  ([data algo]
                   (key-pair/get-signature kp data algo)))
                :get-alg-name
                (fn []
                  (-> (get-in keys/keys [:rsa-nopassphrase :public])
                      (string/split #" ")
                      first))
                :is-encrypted
                (fn []
                  (key-pair/is-encrypted kp))})
        id-repo (identity-repository/new
                 {:get-identities
                  (fn []
                    [ident])})
        _ (agent/set-identity-repository agent id-repo)
        session (agent/get-session agent "root" "localhost" port)]
    (is (= id-repo (agent/get-identity-repository agent)))
    (do-connection session))

  (docker/cleanup)
  )

(deftest identity-repository-encrypted-key
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port port})

  (docker/exec "mkdir /root/.ssh")
  (docker/put-file
   (get-in keys/keys [:rsa-passphrase :public])
   "/root/.ssh/authorized_keys")

  (let [agent (agent/new)
        kp (keys/create-key-pair agent :rsa-passphrase)
        ident (identity/new
               {:set-passphrase
                (fn [_passphrase]
                  false)
                :get-public-key-blob
                (fn []
                  (key-pair/get-public-key-blob kp))
                :get-signature
                (fn
                  ([data]
                   (key-pair/get-signature kp data))
                  ([data algo]
                   (key-pair/get-signature kp data algo)))
                :get-alg-name
                (fn []
                  (-> (get-in keys/keys [:rsa-nopassphrase :public])
                      (string/split #" ")
                      first))
                :is-encrypted
                (fn []
                  (key-pair/is-encrypted kp))})
        id-repo (identity-repository/new
                 {:get-identities
                  (fn []
                    [ident])})
        _ (agent/set-identity-repository agent id-repo)
        session (agent/get-session agent "root" "localhost" port)]
    (is (= id-repo (agent/get-identity-repository agent)))

    ;; decrypt the key
    (key-pair/decrypt
     kp
     (.getBytes
      (get-in keys/keys [:rsa-passphrase :passphrase])))

    (do-connection session))

  (docker/cleanup)
  )
