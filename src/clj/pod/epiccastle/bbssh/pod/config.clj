(ns pod.epiccastle.bbssh.pod.config
  (:require [bbssh.impl.references :as references]
            [bbssh.impl.utils :as utils]
            [pod.epiccastle.bbssh.pod.callbacks :as callbacks]
            [pod.epiccastle.bbssh.pod.cleaner :as cleaner])
  (:import [com.jcraft.jsch ConfigRepository$Config]))

;; pod.epiccastle.bbssh.pod.* are invoked on pod side.

(set! *warn-on-reflection* true)

(defn ^:async new [reply-fn]
  (let [result
        (references/add-instance
         (proxy [ConfigRepository$Config] []
           (getHostname []
             (callbacks/call-method reply-fn :get-hostname []))
           (getUser []
             (callbacks/call-method reply-fn :get-user []))
           (getPort []
             (callbacks/call-method reply-fn :get-port []))
           (getValue [key]
             (callbacks/call-method reply-fn :get-value [key]))
           (getValues [key]
             (into-array
              String
              (callbacks/call-method reply-fn :get-values [key])))))]
    (cleaner/register-delete-fn result #(reply-fn [:done] ["done"]))
    (reply-fn [:result result])
    nil))

(defn get-hostname [config]
  (.getHostname
   ^ConfigRepository$Config (references/get-instance config)))

(defn get-user [config]
  (.getUser
   ^ConfigRepository$Config (references/get-instance config)))

(defn get-port [config]
  (.getPort
   ^ConfigRepository$Config (references/get-instance config)))

(defn get-value [config key]
  (.getValue
   ^ConfigRepository$Config (references/get-instance config)
   ^String key))

(defn get-values [config key]
  (.getValues
   ^ConfigRepository$Config (references/get-instance config)
   ^String key))
