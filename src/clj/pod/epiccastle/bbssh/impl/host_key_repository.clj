(ns pod.epiccastle.bbssh.impl.host-key-repository
  (:require [bbssh.impl.references :as references]
            [bbssh.impl.utils :as utils]
            [pod.epiccastle.bbssh.impl.callbacks :as callbacks]
            [pod.epiccastle.bbssh.impl.cleaner :as cleaner])
  (:import [com.jcraft.jsch HostKeyRepository HostKey UserInfo]))

;; pod.epiccastle.bbssh.impl.* are invoked on pod side.

(set! *warn-on-reflection* true)

(defn ^:async new [reply-fn]
  (let [result
        (references/add-instance
         (proxy [HostKeyRepository] []
           (check [^String host ^bytes key]
             ({:ok 0
               :not-included 1
               :changed 2}
              (callbacks/call-method
               reply-fn :check
               [host (utils/encode-base64 key)])))
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
             ([^String host ^String type ^bytes key]
              (callbacks/call-method
               reply-fn :remove
               [host type (utils/encode-base64 key)])))
           (getKnownHostsRepositoryID []
             (callbacks/call-method reply-fn :get-known-hosts-repository-id []))
           (getHostKey
             ([]
              (->>
               (callbacks/call-method reply-fn :get-host-key [])
               (mapv references/get-instance)
               make-array))
             ([^String host ^String type]
              (->>
               (callbacks/call-method reply-fn :get-host-key [host type])
               (mapv references/get-instance)
               make-array)))))]
    (cleaner/register-delete-fn result #(reply-fn [:done] ["done"]))
    (reply-fn [:result result])
    nil))

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
