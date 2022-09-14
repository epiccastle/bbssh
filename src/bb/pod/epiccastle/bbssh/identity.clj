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
  ```

    Called when the system wants to try to decrypt this identity
    with the passed in passphrase.

  ```clojure
  :get-public-key-blob (fn [] ...)
  ```

    Return a byte-array of the identity's public key.

  ```clojure
  :get-signature (fn ([^bytes data] ...)
                     ([^bytes data ^String algorithm))
  ```

    Sign the incoming data (with algorithm) and return a byte-array
    of the signature

  ```clojure
  :get-alg-name (fn [] ...)
  ```

    Return a string containing the identity's algorithm name. for example
    \"ssh-rsa\" or \"ssh-dss\"

  ```clojure
  :get-name (fn [] ...)
  ```

    Return a string name for this identity

  ```clojure
  :is-encrypted (fn [] ...)
  ```

    Return a truthy value if this identity is encrypted

  ```clojure
  :clear (fn [] ...)
  ```
    Erase all the memory associated with this identity as the system
    has finished using it.

  "
  [callbacks]
  (utils/new-invoker
   {:call-sym 'pod.epiccastle.bbssh.impl.identity/new
    :args []
    :callbacks callbacks
    :preprocess-args-fn preprocess-args
    :postprocess-returns-fn postprocess-returns}))
