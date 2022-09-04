(ns pod.epiccastle.bbssh.impl.key-pair
  (:require [bbssh.impl.references :as references]
            [bbssh.impl.utils :as utils])
  (:import [com.jcraft.jsch
            JSch KeyPair
            ;; KeyPair$DSA
            ;; KeyPair$RSA
            ;; KeyPair$ECDSA
            ;; KeyPair$UNKNOWN
            ;; KeyPair$ED25519
            ;; KeyPair$ED448
            ])
  )

;; pod.epiccastle.bbssh.impl.* are invoked on pod side.

(set! *warn-on-reflection* true)

(defn generate [agent key-type key-size]
  (references/add-instance
   (KeyPair/genKeyPair
    ^JSch (references/get-instance agent)
    ^int ({:dsa KeyPair/DSA
           :rsa KeyPair/RSA
           :ecdsa KeyPair/ECDSA
           :ed25519 KeyPair/ED25519
           :ed448 KeyPair/ED448}
          key-type)
    ^int key-size)))


(defn set-passphrase [key-pair passphrase]
  (.setPassphrase
   ^KeyPair (references/get-instance key-pair)
   ^String passphrase))

(defn write-private-key [key-pair filename]
  (.writePrivateKey
   ^KeyPair (references/get-instance key-pair)
   ^String filename))

(defn write-public-key [key-pair filename comment]
  (.writePublicKey
   ^KeyPair (references/get-instance key-pair)
   ^String filename
   ^String comment))
