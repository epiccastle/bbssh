(ns bb-test.test-key-pair
  (:require [pod.epiccastle.bbssh.agent :as agent]
            [pod.epiccastle.bbssh.session :as session]
            [pod.epiccastle.bbssh.channel-exec :as channel-exec]
            [pod.epiccastle.bbssh.input-stream :as input-stream]
            [pod.epiccastle.bbssh.output-stream :as output-stream]
            [pod.epiccastle.bbssh.key-pair :as key-pair]
            [pod.epiccastle.bbssh.identity :as identity]
            [pod.epiccastle.bbssh.identity-repository :as identity-repository]
            [pod.epiccastle.bbssh.cleaner :as cleaner]
            [bb-test.docker :as docker]
            [bb-test.keys :as keys]
            [clojure.test :refer [is deftest]]
            [clojure.string :as string]))

(defn create-key-pair [agent key-id]
  (spit "/tmp/bbssh-test-key" (get-in keys/keys [key-id :private]))
  (spit "/tmp/bbssh-test-key.pub" (get-in keys/keys [key-id :public]))
  (key-pair/load agent "/tmp/bbssh-test-key" "/tmp/bbssh-test-key.pub"))

(deftest identity-repository
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})

  (let [agent (agent/new)
        kp (create-key-pair agent :rsa-nopassphrase)]
    (is (= (key-pair/get-finger-print kp)
           "e6:a0:ce:eb:00:0c:6d:a3:8f:b6:cb:c0:ea:47:74:74"))
    (is (= (key-pair/get-key-size kp)
           1024))
    (is (= (seq (key-pair/get-public-key-blob kp))
           (get-in keys/keys [:rsa-nopassphrase :public-blob])))
    (is (not (key-pair/is-encrypted kp)))

    (let [kp (create-key-pair agent :rsa-passphrase)]
      (is (= (key-pair/get-finger-print kp)
             "58:a5:65:1c:8c:12:c9:6e:67:aa:70:e6:11:e3:c3:10"))
      (is (= (key-pair/get-key-size kp)
             1024))
      (is (= (seq (key-pair/get-public-key-blob kp))
             (get-in keys/keys [:rsa-passphrase :public-blob])))
      (is (key-pair/is-encrypted kp))))

  (docker/cleanup)
  )
