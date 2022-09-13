(ns bb-test.test-known-hosts
  (:require [pod.epiccastle.bbssh.agent :as agent]
            [pod.epiccastle.bbssh.session :as session]
            [pod.epiccastle.bbssh.channel-exec :as channel-exec]
            [pod.epiccastle.bbssh.input-stream :as input-stream]
            [pod.epiccastle.bbssh.output-stream :as output-stream]
            [pod.epiccastle.bbssh.key-pair :as key-pair]
            [pod.epiccastle.bbssh.host-key :as host-key]
            [pod.epiccastle.bbssh.known-hosts :as known-hosts]
            [pod.epiccastle.bbssh.identity :as identity]
            [pod.epiccastle.bbssh.identity-repository :as identity-repository]
            [pod.epiccastle.bbssh.cleaner :as cleaner]
            [bb-test.docker :as docker]
            [bb-test.keys :as keys]
            [clojure.test :refer [is deftest]]
            [clojure.string :as string]))

(deftest known-hosts
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})



  #_(let [agent (agent/new)
        kp (keys/create-key-pair agent :rsa-nopassphrase)]
    (is (= (key-pair/get-finger-print kp)
             (get-in keys/keys [:rsa-nopassphrase :fingerprint])))
    (is (= (key-pair/get-key-size kp)
           1024))
    (is (= (seq (key-pair/get-public-key-blob kp))
           (get-in keys/keys [:rsa-nopassphrase :public-blob])))
    (is (not (key-pair/is-encrypted kp)))

    (let [kp (keys/create-key-pair agent :rsa-passphrase)]
      (is (= (key-pair/get-finger-print kp)
             (get-in keys/keys [:rsa-passphrase :fingerprint])))
      (is (= (key-pair/get-key-size kp)
             1024))
      (is (= (seq (key-pair/get-public-key-blob kp))
             (get-in keys/keys [:rsa-passphrase :public-blob])))
      (is (key-pair/is-encrypted kp))))

  (docker/cleanup)
  )
