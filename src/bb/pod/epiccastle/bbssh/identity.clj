(ns pod.epiccastle.bbssh.identity
  (:require [pod.epiccastle.bbssh.utils :as utils]))

(defn- preprocess-args [method args]
  (case method
    :get-signature
    (let [[data & remain] args]
      (concat [(utils/decode-base64 data)] remain))

    args))

(defn- postprocess-returns [method result]
  (case method
    :get-signature
    (utils/encode-base64 result)

    :get-public-key-blob
    (utils/encode-base64 result)

    result))

(defn new
  "Create a new identity. Pass in a hashmap containing the
  functions to execute as values. These functions will be called
  by the internal ssh engine. The hashmap should contain some
  subset of the following keywords:

  ```clojure
  :set-passphrase (fn [^bytes passphrase] ...)
  ;; called when the system wants to try to decrypt this identity
  ;; with the passed in passphrase.

  :get-public-key-blob (fn [] ...)
  ;; return a byte-array of the identity's public key.

  :get-signature (fn ([^bytes data] ...)
                     ([^bytes data ^String algorithm))
  ;; sign the incoming data (with algorithm) and return a byte-array
  ;; of the signature

  :get-alg-name (fn [] ...)
  ;; return a string containing the identity's algorithm name. for example
  ;; \"ssh-rsa\" or \"ssh-dss\"

  :get-name (fn [] ...)
  ;; return a string name for this identity

  :is-encrypted (fn [] ...)
  ;; return a truthy value if this identity is encrypted

  :clear (fn [] ...)
  ;; erase all the memory associated with this identity as the system
  ;; has finished using it.
  ```
  "
  [callbacks]
  (utils/new-invoker
   {:call-sym 'pod.epiccastle.bbssh.impl.identity/new
    :args []
    :callbacks callbacks
    :preprocess-args-fn preprocess-args
    :postprocess-returns-fn postprocess-returns}))
