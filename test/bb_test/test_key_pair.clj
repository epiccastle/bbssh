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

(deftest test-key-load
  (let [agent (agent/new)
        key-id :rsa-nopassphrase]
    (docker/run "rm -rf .test/bbssh-test-key .test/bbssh-test-key.pub" "cannot clean test files")
    (docker/run "mkdir -p .test" "cannor mkdir .test")
    (spit ".test/bbssh-test-key" (get-in keys/keys [key-id :private]))
    (spit ".test/bbssh-test-key.pub" (get-in keys/keys [key-id :public]))
    (let [key (key-pair/load agent ".test/bbssh-test-key" ".test/bbssh-test-key.pub")]
      (is (= 1024 (key-pair/get-key-size key)))
      (is (= (get-in keys/keys [key-id :fingerprint]) (key-pair/get-finger-print key)))
      (is (= (seq (key-pair/get-signature key (byte-array (range 256))))
             '(0 0 0 7 115 115 104 45 114 115 97 0 0 0 -128 -83 20 -101 97 -10 45 49
                 -128 108 -20 -36 -50 70 -31 -74 110 -61 73 40 15 0 60 -59 11 84 -127
                 39 56 -81 55 117 52 77 14 -51 -31 -57 1 -128 29 126 -54 72 -111 118
                 28 -65 18 5 104 55 57 50 -59 57 123 -49 -66 -111 122 -41 -117 -88 -109
                 -17 37 -98 -128 68 1 68 9 81 53 -120 -120 73 -78 -29 -40 74 -20 51 61
                 67 103 70 -31 -20 76 -127 0 1 -102 -32 -91 35 -34 22 18 -31 -111 -96
                 72 20 -84 -65 -73 -119 -31 -66 126 24 71 22 -1 -59 -5 14 -45 -123 4 93
                 -85 25 -88 -114 93))))))

(deftest test-key-load-bytes
  (let [agent (agent/new)
        key-id :rsa-nopassphrase]
    (let [key (key-pair/load-bytes
                agent
                (get-in keys/keys [key-id :private])
                (get-in keys/keys [key-id :public]))]
      (is (= 1024 (key-pair/get-key-size key)))
      (is (= (get-in keys/keys [key-id :fingerprint]) (key-pair/get-finger-print key))))
    (let [key (key-pair/load-bytes
                agent
                (.getBytes (get-in keys/keys [key-id :private]))
                (.getBytes (get-in keys/keys [key-id :public])))]
      (is (= 1024 (key-pair/get-key-size key)))
      (is (= (get-in keys/keys [key-id :fingerprint]) (key-pair/get-finger-print key))))))

(deftest test-key-load-bytes-half-key
  (let [agent (agent/new)
        key-id :rsa-nopassphrase]
    (let [key (key-pair/load-bytes
                agent
                nil
                (.getBytes (get-in keys/keys [key-id :public])))]
      (is (= 1024 (key-pair/get-key-size key)))
      (is (= (get-in keys/keys [key-id :fingerprint]) (key-pair/get-finger-print key))))
    (let [key (key-pair/load-bytes
                agent
                (.getBytes (get-in keys/keys [key-id :private]))
                nil)]
      (is (= 1024 (key-pair/get-key-size key)))
      (is (= (get-in keys/keys [key-id :fingerprint]) (key-pair/get-finger-print key))))))
