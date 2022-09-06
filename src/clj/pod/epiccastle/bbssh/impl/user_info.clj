(ns pod.epiccastle.bbssh.impl.user-info
  (:require [bbssh.impl.references :as references]
            [pod.epiccastle.bbssh.impl.callbacks :as callbacks]
            [pod.epiccastle.bbssh.impl.cleaner :as cleaner])
  (:import [com.jcraft.jsch UserInfo]))

;; pod.epiccastle.bbssh.impl.* are invoked on pod side.

(set! *warn-on-reflection* true)

(defn ^:async new [reply-fn]
  (let [result
        (references/add-instance
         (proxy [UserInfo] []
           (getPassword []
             (callbacks/call-method reply-fn :get-password []))
           (promptYesNo [^String s]
             (boolean
              (callbacks/call-method reply-fn :prompt-yes-no [s])))
           (getPassphrase []
             (callbacks/call-method reply-fn :get-passphase []))
           (promptPassphrase [^String s]
             (boolean
              (callbacks/call-method reply-fn :prompt-passphrase [s])))
           (promptPassword [^String s]
             (boolean
              (callbacks/call-method reply-fn :prompt-password [s])))
           (showMessage [^String s]
             (callbacks/call-method reply-fn :show-message [s]))))]
    (cleaner/register-delete-fn result #(reply-fn [:done] ["done"]))
    (reply-fn [:result result])
    nil))
