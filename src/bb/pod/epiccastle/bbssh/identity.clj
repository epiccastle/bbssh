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

(defn new [callbacks]
  (utils/new-invoker
   {:call-sym 'pod.epiccastle.bbssh.impl.identity/new
    :args []
    :callbacks callbacks
    :preprocess-args-fn preprocess-args
    :postprocess-returns-fn postprocess-returns}))
