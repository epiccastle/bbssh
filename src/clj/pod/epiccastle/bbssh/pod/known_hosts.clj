(ns pod.epiccastle.bbssh.pod.known-hosts
  (:require [bbssh.impl.references :as references]
            [bbssh.impl.utils :as utils])
  (:import [com.jcraft.jsch KnownHosts JSch]))

;; (defn new
;;   [agent]
;;   (references/add-instance
;;    (KnownHosts.
;;     ^JSch (references/get-instance agent))))
