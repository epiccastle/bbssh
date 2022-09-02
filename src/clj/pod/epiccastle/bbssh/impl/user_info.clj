(ns pod.epiccastle.bbssh.impl.user-info
  (:require [bbssh.impl.references :as references])
  (:import [com.jcraft.jsch UserInfo]))

;; pod.epiccastle.bbssh.impl.* are invoked on pod side.

(set! *warn-on-reflection* true)

(def sync-register (atom {}))

(defn sync-result [bb-id result]
  ;;(prn 'register @sync-register)
  (let [p (@sync-register bb-id)]
    (deliver p  result)
    nil))

(defn- sync-method [reply-fn method args]
  (let [bb-id (str (gensym "user-info"))
        p (promise)]
    (swap! sync-register assoc bb-id p)
    (reply-fn [:method {:id bb-id
                        :method method
                        :args args}])
    (let [res @p]
      (swap! sync-register dissoc bb-id)
      res)))

(defn ^:async new [reply-fn]
  (let [result
        (references/add-instance
         (proxy [UserInfo] []
           (getPassword []
             (sync-method reply-fn :get-password []))
           (promptYesNo [^String s]
             (sync-method reply-fn :prompt-yes-no [s]))
           (getPassphrase []
             (sync-method reply-fn :get-passphase []))
           (promptPassphrase [^String s]
             (sync-method reply-fn :prompt-passphrase [s]))
           (promptPassword [^String s]
             (sync-method reply-fn :prompt-password [s]))
           (showMessage [^String s]
             (sync-method reply-fn :show-message [s]))))]
    (reply-fn [:result result])
    nil))
