(ns bb-test.test-key-pair
  (:require [pod.epiccastle.bbssh.agent :as agent]
            [pod.epiccastle.bbssh.key-pair :as key-pair]
            [bb-test.docker :as docker]
            [bb-test.keys :as keys]
            [clojure.test :refer [is deftest]]))

(deftest test-key-pair
  (let [agent (agent/new)
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
      (is (key-pair/is-encrypted kp)))))

(deftest test-ed25519
  (let [agent (agent/new)
        kp (key-pair/generate agent :ed25519)]
    (is (= 32 (key-pair/get-key-size kp)))))

(deftest test-ed448
  (let [agent (agent/new)
        kp (key-pair/generate agent :ed448)]
    (is (= 57 (key-pair/get-key-size kp)))))

(deftest test-ecdsa
  (let [agent (agent/new)
        kp (key-pair/generate agent :ecdsa 256)]
    (is (= 256 (key-pair/get-key-size kp)))))

(deftest test-dsa
  (let [agent (agent/new)
        kp (key-pair/generate agent :dsa 1024)]
    (is (= 1024 (key-pair/get-key-size kp)))))

(deftest test-rsa
  (let [agent (agent/new)
        kp (key-pair/generate agent :rsa 1024)]
    (is (= 1024 (key-pair/get-key-size kp)))))
