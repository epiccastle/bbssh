(ns pod.epiccastle.bbssh.pod.session
  (:require [bbssh.impl.references :as references]
            [bbssh.impl.utils :as utils])
  (:import [com.jcraft.jsch JSch Session
            UserInfo IdentityRepository
            HostKeyRepository])
  )

;; pod.epiccastle.bbssh.impl.* are invoked on pod side.

(set! *warn-on-reflection* true)

(defn set-password [session password]
  (.setPassword
   ^Session (references/get-instance session)
   ^String password))

(defn set-user-info [session user-info]
  (.setUserInfo
   ^Session (references/get-instance session)
   ^UserInfo (references/get-instance user-info)))

(defn ^:blocking connect
  "marked ^:blocking because connect blocks until the connection
  is made. This process may need many async callbacks via user-info
  and identity stores"
  [session & [timeout]]
  (if timeout
    (.connect
     ^Session (references/get-instance session)
     timeout)
    (.connect
     ^Session (references/get-instance session))))

(defn disconnect [session]
  (.disconnect
   ^Session (references/get-instance session)))

(defn set-port-forwarding-local
  [session
   {:keys [bind-address
           local-port
           remote-host
           remote-port
           connect-timeout]
    :or {bind-address "127.0.0.1"}}]
  (.setPortForwardingL
   ^Session (references/get-instance session)
   ^String bind-address
   ^int local-port
   ^String remote-host
   ^int remote-port
   nil
   ^int connect-timeout)

  )

(defn delete-port-forwarding-local
  [session
   {:keys [bind-address
           local-port
           remote-host
           remote-port
           connect-timeout]
    :or {bind-address "127.0.0.1"}}]
  (.setPortForwardingL
   ^Session (references/get-instance session)
   ^String bind-address
   ^int local-port
   ^String remote-host
   ^int remote-port
   nil
   ^int connect-timeout)

  )

(defn get-port-forwarding-local
  [session]
  (.getPortForwardingL
   ^Session (references/get-instance session)))

(defn set-host
  [session host]
  (.setHost
   ^Session (references/get-instance session)
   ^String host))

(defn set-port
  [session port]
  (.setHost
   ^Session (references/get-instance session)
   ^int port))

(defn set-config
  [session key value]
  (.setConfig
   ^Session (references/get-instance session)
   ^String (if (keyword? key)
             (utils/to-camel-case (name key))
             key)
   ^String (utils/boolean-to-yes-no value)))

(defn set-configs
  [session hashmap]
  (doseq [[key value] hashmap]
    (.setConfig
     ^Session (references/get-instance session)
     ^String (if (keyword? key)
               (utils/to-camel-case (name key))
               key)
     ^String (utils/boolean-to-yes-no value))))

(defn get-config
  [session key]
  (.getConfig
   ^Session (references/get-instance session)
   ^String (if (keyword? key)
             (utils/to-camel-case (name key))
             key)))

(defn connected?
  [session]
  (.isConnected
   ^Session (references/get-instance session)))

(defn open-channel
  [session type]
  (references/add-instance
   (.openChannel
    ^Session (references/get-instance session)
    ^String type)))

(defn set-identity-repository
  [session identity-repository]
  (.setIdentityRepository
   ^Session (references/get-instance session)
   ^IdentityRepository (references/get-instance identity-repository)))

(defn set-host-key-repository
  [session host-key-repository]
  (.setHostKeyRepository
   ^Session (references/get-instance session)
   ^HostKeyRepository (references/get-instance host-key-repository)))
