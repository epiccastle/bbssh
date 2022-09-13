(ns pod.epiccastle.bbssh.impl.agent
  (:require [bbssh.impl.references :as references]
            [bbssh.impl.utils :as utils])
  (:import [com.jcraft.jsch JSch
            IdentityRepository HostKeyRepository
            ConfigRepository Identity])
  )

;; pod.epiccastle.bbssh.impl.* are invoked on pod side.

(set! *warn-on-reflection* true)

(defn new []
  (references/add-instance
   (JSch.)))

(defn get-session
  ([agent host]
   (references/add-instance
    (.getSession
     ^JSch (references/get-instance agent)
     ^String host)))
  ([agent username host]
   (references/add-instance
    (.getSession
     ^JSch (references/get-instance agent)
     ^String username
     ^String host)))
  ([agent username host port]
   (references/add-instance
    (.getSession
     ^JSch (references/get-instance agent)
     ^String username
     ^String host
     ^int port))))

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

(defn get-config-repository
  [agent]
  (references/add-instance
   (.getIdentityRepository
    ^JSch (references/get-instance agent))))

(defn set-config-repository
  [agent config-repository]
  (.setIdentityRepository
   ^JSch (references/get-instance agent)
   ^ConfigRepository (references/get-instance config-repository)))

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

(defn set-known-hosts
  [agent filename]
  (.setKnownHosts
   ^JSch (references/get-instance agent)
   ^String filename))

(defn add-identity
  ([agent filename]
   (.addIdentity
    ^JSch (references/get-instance agent)
    ^String filename))
  ([agent filename passphrase]
   (.addIdentity
    ^JSch (references/get-instance agent)
    ^String filename
    ^String passphrase))
  ([agent private-key-filename public-key-filename passphrase]
   (.addIdentity
    ^JSch (references/get-instance agent)
    ^String private-key-filename
    ^String public-key-filename
    ^bytes (utils/decode-base64 passphrase)))
  ([agent identity-name private-key public-key passphrase]
   (.addIdentity
    ^JSch (references/get-instance agent)
    ^String identity-name
    ^bytes (utils/decode-base64 private-key)
    ^bytes (utils/decode-base64 public-key)
    ^bytes (utils/decode-base64 passphrase))))

(defn add-identity2
  [agent filename passphrase]
  (.addIdentity
   ^JSch (references/get-instance agent)
   ^String filename
   ^bytes (utils/decode-base64 passphrase)))

(defn add-identity3
  [agent identity passphrase]
  (.addIdentity
   ^JSch (references/get-instance agent)
   ^Identity (references/get-instance identity)
   ^bytes (utils/decode-base64 passphrase)))

(defn remove-identity
  [agent identity-name]
  (.removeIdentity
   ^JSch (references/get-instance agent)
   ^String identity-name))

(defn remove-identity2
  [agent identity]
  (.removeIdentity
   ^JSch (references/get-instance agent)
   ^Identity (references/get-instance identity)))

(defn get-identity-names
  [agent]
  (into []
        (.getIdentityNames
         ^JSch (references/get-instance agent))))

(defn remove-all-identities
  [agent]
  (.removeAllIdentity
   ^JSch (references/get-instance agent)))

