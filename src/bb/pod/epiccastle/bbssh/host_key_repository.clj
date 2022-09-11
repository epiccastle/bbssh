(ns pod.epiccastle.bbssh.host-key-repository
  (:require [pod.epiccastle.bbssh.impl.host-key-repository :as host-key-repository]
            [pod.epiccastle.bbssh.cleaner :as cleaner]
            [pod.epiccastle.bbssh.utils :as utils]))

(defn- preprocess-args [method args]
  (case method
    :check
    (let [[host key] args]
      [host (utils/decode-base64 key)])

    :add
    (let [[host-key user-info] args]
      [(cleaner/register host-key)
       (cleaner/register user-info)])

    :remove
    (let [[host type key] args]
      (if key
        [host type (utils/decode-base64 key)]
        [host key]))

    args))

(defn- postprocess-returns [method result]
  (case method
    :get-host-key
    (mapv cleaner/split-key result)

    result))

(defn new [callbacks]
  (utils/new-invoker
   {:call-sym 'pod.epiccastle.bbssh.impl.host-key-repository/new
    :args []
    :callbacks callbacks
    :preprocess-args-fn preprocess-args
    :postprocess-returns-fn postprocess-returns}))

(defn get-host-key
  ([host-key-repository]
   (mapv cleaner/register
         (host-key-repository/get-host-key
          (cleaner/split-key host-key-repository))))
  ([host-key-repository host type]
   (mapv cleaner/register
         (host-key-repository/get-host-key
          (cleaner/split-key host-key-repository)
          host type))))

(defn get-known-hosts-repository-id [host-key-repository]
  (host-key-repository/get-known-hosts-repository-id
   (cleaner/split-key host-key-repository)))
