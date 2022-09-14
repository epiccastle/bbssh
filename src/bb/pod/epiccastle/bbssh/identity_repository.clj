(ns pod.epiccastle.bbssh.identity-repository
  (:require [pod.epiccastle.bbssh.cleaner :as cleaner]
            [pod.epiccastle.bbssh.utils :as utils]))

(defn- preprocess-args [method args]
  (case method
    :add
    (let [[data] args]
      [(utils/decode-base64 data)])

    :remove
    (let [[data] args]
      [(utils/decode-base64 data)])

    args))

(defn- postprocess-returns [method result]
  (case method
    :get-identities
    (mapv cleaner/split-key result)

    result))

(defn new
  "Create a new identity-repository. Pass in a hashmap containing
  the functions to execute as values. These functions will be called
  by the internal ssh engine. The hashmap should contain some subset
  of the the following keywords:

  ```clojure
  :get-name (fn [] ...)
  ```
    return a string specifying the name of this repository

  ```clojure
  :get-status (fn [] ...)
  ```
    return the present status of this repository. Can be :unavailable,
    :not-running or :running

  ```clojure
  :get-identities (fn [] ...)
  ```
    return a sequence of the identities stored in this repository

  ```clojure
  :add (fn [^bytes identity-data] ...)
  ```
    add the passed in raw data as an identity

  ```clojure
  :remove (fn [^bytes identity-data] ...)
  ```
    remove the passed in raw data identity from the repository.

  ```clojure
  :removeAll (fn [] ...)
  ```
    empty the repository

  "
  [callbacks]
  (utils/new-invoker
   {:call-sym 'pod.epiccastle.bbssh.impl.identity-repository/new
    :args []
    :callbacks callbacks
    :preprocess-args-fn preprocess-args
    :postprocess-returns-fn postprocess-returns}))
