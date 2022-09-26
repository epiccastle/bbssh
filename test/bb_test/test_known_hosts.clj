(ns bb-test.test-known-hosts
  (:require [pod.epiccastle.bbssh.agent :as agent]
            [pod.epiccastle.bbssh.user-info :as user-info]
            [pod.epiccastle.bbssh.host-key :as host-key]
            [pod.epiccastle.bbssh.host-key-repository :as host-key-repository]
            [pod.epiccastle.bbssh.impl.utils :as utils]
            [bb-test.keys :as keys]
            [clojure.test :refer [is deftest]]))

(defn make-salt []
  (->> (repeatedly #(rand-int 256))
       (take 20)
       byte-array))

#_ (make-salt)

(defn make-hash [salt hostname]
  (let [host-bytes (.getBytes hostname)
        hash (byte-array 20)]
    (doto (javax.crypto.Mac/getInstance "HmacSHA1")
      (.init (javax.crypto.spec.SecretKeySpec. salt "HmacSHA1"))
      (.update host-bytes 0 (count host-bytes))
      (.doFinal hash 0))
    hash))

#_ (make-hash (make-salt) "hostname.domain")

(defn hash-hostname [hostname]
  (let [salt (make-salt)
        hash (make-hash salt hostname)]
    (str "|1|" (utils/encode-base64 salt) "|" (utils/encode-base64 hash))))

(deftest known-hosts
  (let [agent (agent/new)
        host "192.168.100.1"
        host-hash (hash-hostname host)
        public-b64 (keys/get-public-key-base64 :rsa-nopassphrase)]
    (agent/set-known-hosts-content
     agent
     (->
      (str host-hash
           " ssh-rsa "
           public-b64
           " some comment 🚀 \r\n"
           "# 192.168.100.1 ssh-rsa MYRSAKEY some other comment ☂")
      (.getBytes "UTF-8"))
     )
    (let [hkr (agent/get-host-key-repository agent)
          keys (host-key-repository/get-host-key hkr)]
      (is (= 1 (count keys)))
      (is (= (host-key/get-info (first keys) agent)
             {:host host-hash
              :type "ssh-rsa"
              :key public-b64
              :finger-print (get-in keys/keys [:rsa-nopassphrase :fingerprint-256])
              :comment "some comment ￰ﾟﾚﾀ " ;; JSch interprets file as ascii?
              :marker ""}))

      ;; check key is there
      (is (= :ok
             (host-key-repository/check
              hkr
              host
              (utils/decode-base64 public-b64))))

      ;; check with a different key
      (is (= :changed
             (host-key-repository/check
              hkr
              host
              (utils/decode-base64
               (keys/get-public-key-base64 :rsa-passphrase)))))

      ;; remove key
      (host-key-repository/remove hkr host "ssh-rsa")
      (is (= :not-included
             (host-key-repository/check
              hkr
              host
              (utils/decode-base64 public-b64))))

      (is (empty? (host-key-repository/get-host-key hkr)))

      (let [new-key (utils/decode-base64 (keys/get-public-key-base64 :dsa-no-passphrase))
            new-host "host.domain"]
        ;; add key
        (host-key-repository/add
         hkr
         (host-key/new new-host new-key)
         (user-info/new
          {:get-password (fn [_] (prn :get-password))
           :get-passphrase (fn [_] (prn :get-passphrase))
           :prompt-yes-no (fn [_] (prn :prompt-yes-no))
           :prompt-password (fn [_] (prn :prompt-password))
           :prompt-passphrase (fn [_] (prn :prompt-passphrase))
           :show-message (fn [_] (prn :show-message))}))

        (is (= 1 (count (host-key-repository/get-host-key hkr))))

        ;; check key is there
        (is (= :ok
               (host-key-repository/check
                hkr
                new-host
                new-key)))

        ;; check with a different key
        (is (= :changed
               (host-key-repository/check
                hkr
                new-host
                (utils/decode-base64
                 (keys/get-public-key-base64 :dsa-no-passphrase-2)))))

        ;; different type of key wont be marked as changed
        (is (= :not-included
               (host-key-repository/check
                hkr
                new-host
                (utils/decode-base64
                 (keys/get-public-key-base64 :rsa-nopassphrase)))))

        (host-key-repository/remove hkr new-host "ssh-dss")
        (is (= :not-included
               (host-key-repository/check
                hkr
                new-host
                new-key)))

        (is (empty? (host-key-repository/get-host-key hkr))))

      (let [new-key (utils/decode-base64 (keys/get-public-key-base64 :ecdsa-no-passphrase))
            new-host "10.0.0.1"]
        ;; add key
        (host-key-repository/add
         hkr
         (host-key/new new-host new-key)
         (user-info/new {}))

        (is (= 1 (count (host-key-repository/get-host-key hkr))))

        ;; check key is there
        (is (= :ok
               (host-key-repository/check
                hkr
                new-host
                new-key))))

      (let [new-key (utils/decode-base64 (keys/get-public-key-base64 :ed25519-no-passphrase))
            new-host "10.0.0.1"]
        ;; add key
        (host-key-repository/add
         hkr
         (host-key/new new-host new-key)
         (user-info/new {}))

        (is (= 2 (count (host-key-repository/get-host-key hkr))))

        ;; check key is there
        (is (= :ok
               (host-key-repository/check
                hkr
                new-host
                new-key))))


      )))
