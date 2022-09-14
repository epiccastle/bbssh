(ns bb-test.test-known-hosts
  (:require [pod.epiccastle.bbssh.agent :as agent]
            [pod.epiccastle.bbssh.session :as session]
            [pod.epiccastle.bbssh.channel-exec :as channel-exec]
            [pod.epiccastle.bbssh.input-stream :as input-stream]
            [pod.epiccastle.bbssh.output-stream :as output-stream]
            [pod.epiccastle.bbssh.key-pair :as key-pair]
            [pod.epiccastle.bbssh.host-key :as host-key]
            [pod.epiccastle.bbssh.known-hosts :as known-hosts]
            [pod.epiccastle.bbssh.host-key-repository :as host-key-repository]
            [pod.epiccastle.bbssh.identity :as identity]
            [pod.epiccastle.bbssh.utils :as utils]
            [pod.epiccastle.bbssh.identity-repository :as identity-repository]
            [pod.epiccastle.bbssh.cleaner :as cleaner]
            [bb-test.docker :as docker]
            [bb-test.keys :as keys]
            [clojure.test :refer [is deftest]]
            [clojure.string :as string]))

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
              :marker ""}
             ))
      ;; check key is there
      (is (= :ok (host-key-repository/check
                  hkr
                  host
                  (utils/decode-base64 public-b64))))
      ;; check with a different key
      (is (= :changed (host-key-repository/check
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
              (utils/decode-base64 public-b64)))))))
