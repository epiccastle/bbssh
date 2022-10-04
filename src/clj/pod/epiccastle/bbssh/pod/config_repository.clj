(ns pod.epiccastle.bbssh.pod.config-repository
  (:require [bbssh.impl.references :as references]
            [bbssh.impl.utils :as utils]
            [pod.epiccastle.bbssh.pod.callbacks :as callbacks]
            [pod.epiccastle.bbssh.pod.cleaner :as cleaner])
  (:import [com.jcraft.jsch ConfigRepository OpenSSHConfig]
           [java.util Vector]))

;; pod.epiccastle.bbssh.pod.* are invoked on pod side.

(set! *warn-on-reflection* true)

(defn ^:async new [reply-fn]
  (let [result
        (references/add-instance
         (proxy [ConfigRepository] []
           (getConfig [hostname]
             (references/get-instance
              (callbacks/call-method reply-fn :get-config [hostname])))))]
    (cleaner/register-delete-fn result #(reply-fn [:done] ["done"]))
    (reply-fn [:result result])
    nil))

(defn get-config [config-repository hostname]
  (.getConfig
   ^ConfigRepository (references/get-instance config-repository)
   ^String hostname))

(defn openssh-config-file [config-file]
  (references/add-instance
   (OpenSSHConfig/parseFile config-file)))

(defn openssh-config-string [config]
  (references/add-instance
   (OpenSSHConfig/parse config)))
