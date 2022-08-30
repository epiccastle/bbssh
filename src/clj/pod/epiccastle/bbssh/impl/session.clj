(ns pod.epiccastle.bbssh.impl.session
  (:require [bbssh.impl.references :as references])
  (:import [com.jcraft.jsch JSch Session])
  )

;; pod.epiccastle.bbssh.impl.* are invoked on pod side.

(set! *warn-on-reflection* true)

(defn new [agent username host port]
  #_(references/add-instance
   (Session.
    ^com.jcraft.jsch.Jsch (references/get-instance agent)
    ^String username
    ^String host
    ^int port)))

(defn set-password [session password]
  (.setPassword
   ^Session (references/get-instance session)
   ^String password))