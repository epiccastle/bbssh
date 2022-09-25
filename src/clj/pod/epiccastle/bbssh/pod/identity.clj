(ns pod.epiccastle.bbssh.pod.identity
  (:require [bbssh.impl.references :as references]
            [bbssh.impl.utils :as utils]
            [pod.epiccastle.bbssh.impl.callbacks :as callbacks]
            [pod.epiccastle.bbssh.impl.cleaner :as cleaner])
  (:import [com.jcraft.jsch Identity]))

;; pod.epiccastle.bbssh.impl.* are invoked on pod side.

(set! *warn-on-reflection* true)

(defn ^:async new [reply-fn]
  (let [result
        (references/add-instance
         (proxy [Identity] []
           (setPassphrase [^bytes passphrase]
             (callbacks/call-method
              reply-fn :set-passphrase
              [(some-> passphrase utils/encode-base64)]))
           (getPublicKeyBlob []
             (utils/decode-base64
              (callbacks/call-method reply-fn :get-public-key-blob [])))
           (getSignature
             ([^bytes data]
              (utils/decode-base64
               (callbacks/call-method
                reply-fn :get-signature
                [(some-> data utils/encode-base64)])))
             ([^bytes data ^String alg]
              (utils/decode-base64
               (callbacks/call-method
                reply-fn :get-signature
                [(some-> data utils/encode-base64) alg]))))
           ;; deprecated in JSch
           #_(decrypt []
             (callbacks/call-method reply-fn :decrypt []))
           (getAlgName []
             (callbacks/call-method reply-fn :get-alg-name []))
           (getName []
             (callbacks/call-method reply-fn :get-name []))
           (isEncrypted []
             (boolean
              (callbacks/call-method reply-fn :is-encrypted [])))
           (clear []
             (callbacks/call-method reply-fn :clear []))))]
    (cleaner/register-delete-fn result #(reply-fn [:done] ["done"]))
    (reply-fn [:result result])
    nil))
