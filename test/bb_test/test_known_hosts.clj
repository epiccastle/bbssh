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

(def test-key
  {:rsa-key "AAAAB3NzaC1yc2EAAAABIwAAAQEAq2A7hRGmdnm9tUDbO9IDSwBK6TbQa+PXYPCPy6rbTrTtw7PHkccKrpp0yVhp5HdEIcKr6pLlVDBfOLX9QUsyCOV0wzfjIJNlGEYsdlLJizHhbn2mUjvSAHQqZETYP81eFzLQNnPHt4EVVUh7VfDESU84KezmD5QlWpXLmvU31/yMf+Se8xhHTvKSCZIFImWwoG6mbUoWf9nzpIoaSjB+weqqUUmpaaasXVal72J+UX2B+2RPW3RcT0eOzQgqlJL3RKrTJvdsjE3JEAvGq3lGHSZXy28G3skua2SmVi/w4yCE6gbODqnTWlg7+wC604ydGXA8VJiS5ap43JXiUFFAaQ=="
   :hash-value "|1|F1E1KeoE/eEWhi10WpGv4OdiO6Y=|3988QV0VE8wmZL7suNrYQLITLCg="
   :host-line "192.168.1.61"
   })

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
        host-hash
        #_"|1|F1E1KeoE/eEWhi10WpGv4OdiO6Y=|3988QV0VE8wmZL7suNrYQLITLCg="
        (hash-hostname "192.168.1.61")]
    (agent/set-known-hosts-content
     agent
     (->
      (str host-hash
           " ssh-rsa "
           (:rsa-key test-key)
           " some comment 🚀 \r\n"
           "# 192.168.1.61 ssh-rsa MYRSAKEY some other comment ☂")
      (.getBytes "UTF-8"))
     )
    (let [hkr (agent/get-host-key-repository agent)
          keys (host-key-repository/get-host-key hkr)]
      (is (= 1 (count keys)))
      (is (= (host-key/get-info (first keys) agent)
             {:host host-hash
              :type "ssh-rsa"
              :key (:rsa-key test-key)
              :finger-print "9d:38:5b:83:a9:17:52:92:56:1a:5e:c4:d4:81:8e:0a:ca:51:a2:64:f1:74:20:11:2e:f8:8a:c3:a1:39:49:8f"
              :comment "some comment ￰ﾟﾚﾀ " ;; JSch interprets file as ascii?
              :marker ""}
             ))
      (is (= :ok (host-key-repository/check
                  hkr
                  "192.168.1.61"
                  (utils/decode-base64 (:rsa-key test-key)))))
      (is (= :changed (host-key-repository/check
                       hkr
                       "192.168.1.61"
                       (utils/decode-base64 "AAAAC3NzaC1yc2EAAAABIwAAAQEAq2A7hRGmdnm9tUDbO9IDSwBK6TbQa+PXYPCPy6rbTrTtw7PHkccKrpp0yVhp5HdEIcKr6pLlVDBfOLX9QUsyCOV0wzfjIJNlGEYsdlLJizHhbn2mUjvSAHQqZETYP81eFzLQNnPHt4EVVUh7VfDESU84KezmD5QlWpXLmvU31/yMf+Se8xhHTvKSCZIFImWwoG6mbUoWf9nzpIoaSjB+weqqUUmpaaasXVal72J+UX2B+2RPW3RcT0eOzQgqlJL3RKrTJvdsjE3JEAvGq3lGHSZXy28G3skua2SmVi/w4yCE6gbODqnTWlg7+wC604ydGXA8VJiS5ap43JXiUFFAaR=="))))

      (host-key-repository/remove hkr "192.168.1.61" "ssh-rsa")
      (is (= :not-included
             (host-key-repository/check
              hkr
              "192.168.1.61"
              (utils/decode-base64 (:rsa-key test-key)))))
      )

    ))
