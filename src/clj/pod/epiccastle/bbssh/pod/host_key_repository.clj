(ns pod.epiccastle.bbssh.pod.host-key-repository
  (:refer-clojure :exclude [remove])
  (:require [bbssh.impl.references :as references]
            [bbssh.impl.utils :as utils]
            [pod.epiccastle.bbssh.pod.callbacks :as callbacks]
            [pod.epiccastle.bbssh.pod.cleaner :as cleaner])
  (:import [com.jcraft.jsch HostKeyRepository HostKey UserInfo]))

;; pod.epiccastle.bbssh.pod.* are invoked on pod side.

(set! *warn-on-reflection* true)

(defn ^:async new [reply-fn]
  (let [result
        (references/add-instance
         (proxy [HostKeyRepository] []
           (check [^String host ^bytes public-key]
             ({:ok 0
               :not-included 1
               :changed 2}
              (callbacks/call-method
               reply-fn :check
               [host (utils/encode-base64 public-key)])))
           (add [^HostKey host-key ^UserInfo user-info]
             (callbacks/call-method
              reply-fn :add
              [(references/add-instance host-key)
               (references/add-instance user-info)]))
           (remove
             ([^String host ^String type]
              (callbacks/call-method
               reply-fn :remove
               [host type]))
             ([^String host ^String type ^bytes public-key]
              (callbacks/call-method
               reply-fn :remove
               [host type (some-> public-key utils/encode-base64)])))
           (getKnownHostsRepositoryID []
             (callbacks/call-method reply-fn :get-known-hosts-repository-id []))
           (getHostKey
             ([]
              (->>
               (callbacks/call-method reply-fn :get-host-key [])
               (mapv references/get-instance)
               (into-array HostKey)))
             ([^String host ^String type]
              (->>
               (callbacks/call-method reply-fn :get-host-key [host type])
               (mapv references/get-instance)
               (into-array HostKey))))))]
    (cleaner/register-delete-fn result #(reply-fn [:done] ["done"]))
    (reply-fn [:result result])
    nil))

(defn check
  [host-key-repository host key]
  ({0 :ok
    1 :not-included
    2 :changed}
   (.check
    ^HostKeyRepository (references/get-instance host-key-repository)
    ^String host
    (utils/decode-base64 key))))

(defn add
  [host-key-repository host-key user-info]
  (.add
   ^HostKeyRepository (references/get-instance host-key-repository)
   ^HostKey (references/get-instance host-key)
   ^UserInfo (references/get-instance user-info)))

(defn remove
  ([host-key-repository host type]
   (.remove
    ^HostKeyRepository (references/get-instance host-key-repository)
    ^String host
    ^String type))
  ([host-key-repository host type key]
   (.remove
    ^HostKeyRepository (references/get-instance host-key-repository)
    ^String host
    ^String type
    ^bytes (utils/decode-base64 key))))

(defn get-host-key
  ([host-key-repository]
   (mapv references/add-instance
         (.getHostKey
          ^HostKeyRepository (references/get-instance host-key-repository))))
  ([host-key-repository host type]
   (mapv references/add-instance
         (.getHostKey
          ^HostKeyRepository (references/get-instance host-key-repository)
          ^String host
          ^String type))))

(defn get-known-hosts-repository-id [host-key-repository]
  (.getKnownHostsRepositoryID
   ^HostKeyRepository (references/get-instance host-key-repository)))
