(ns bb-test.test-host-key
  (:require [bb-test.docker :as docker]
            [clojure.test :refer [is deftest]]))

(deftest host-key
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
