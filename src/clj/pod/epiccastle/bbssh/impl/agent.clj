(ns pod.epiccastle.bbssh.impl.agent
  (:require [bbssh.impl.references :as references])
  (:import [com.jcraft.jsch JSch IdentityRepository HostKeyRepository])
  )

;; pod.epiccastle.bbssh.impl.* are invoked on pod side.

(set! *warn-on-reflection* true)

(defn new []
  (references/add-instance
   (JSch.)))

(defn get-session [agent username host port]
  (references/add-instance
   (.getSession
    ^JSch (references/get-instance agent)
    ^String username
    ^String host
    ^int port)))

(defn get-identity-repository
  [agent]
  (references/add-instance
   (.getIdentityRepository
    ^JSch (references/get-instance agent))))

(defn set-identity-repository
  [agent identity-repository]
  (.setIdentityRepository
   ^JSch (references/get-instance agent)
   ^IdentityRepository (references/get-instance identity-repository)))

(defn get-host-key-repository
  [agent]
  (references/add-instance
   (.getHostKeyRepository
    ^JSch (references/get-instance agent))))

(defn set-host-key-repository
  [agent host-key-repository]
  (.setHostKeyRepository
   ^JSch (references/get-instance agent)
   ^HostKeyRepository (references/get-instance host-key-repository)))
