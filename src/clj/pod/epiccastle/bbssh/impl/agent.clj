(ns pod.epiccastle.bbssh.impl.agent
  (:require [bbssh.impl.references :as references])
  (:import [com.jcraft.jsch JSch])
  )

;; pod.epiccastle.bbssh.impl.* are invoked on pod side.

(defn new []
  (references/add-instance
   (JSch.)
   "pod.epiccastle.bbssh.agent" "agent"))

(defn get-session [agent username host port]
  (references/add-instance
   (.getSession (references/get-instance agent)
                username
                host
                port)
   "pod.epiccastle.bbssh.session" "session"))
