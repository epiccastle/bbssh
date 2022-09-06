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

(defn new [callbacks]
  (utils/new-invoker
   {:call-sym 'pod.epiccastle.bbssh.impl.identity-repository/new
    :args []
    :callbacks callbacks
    :preprocess-args-fn preprocess-args
    :postprocess-returns-fn postprocess-returns}))
