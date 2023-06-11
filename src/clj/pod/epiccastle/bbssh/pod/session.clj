(ns pod.epiccastle.bbssh.pod.session
  (:require [bbssh.impl.references :as references]
            [bbssh.impl.utils :as utils]
            [clojure.string :as string])
  (:import [com.jcraft.jsch JSch Session
            UserInfo IdentityRepository
            HostKeyRepository Proxy ProxyHTTP ProxySOCKS4 ProxySOCKS5])
  )

;; pod.epiccastle.bbssh.pod.* are invoked on pod side.

(set! *warn-on-reflection* true)

(defn set-password [session password]
  (.setPassword
   ^Session (references/get-instance session)
   ^String password))

(defn set-user-info [session user-info]
  (.setUserInfo
   ^Session (references/get-instance session)
   ^UserInfo (references/get-instance user-info)))

(defn make-proxy
  [{:keys [type host port username password]}]
  (let [proxy (case type
                :http (ProxyHTTP. host port)
                :socks4 (ProxySOCKS4. host port)
                :socks5 (ProxySOCKS5. host port))]
    (when username
      (case type
        ;; Seems we don't have a better way to avoid the code duplication since
        ;; the setUserPasswd method is not in the jsch `Proxy` interface.
        :http (.setUserPasswd ^ProxyHTTP proxy username password)
        :socks4 (.setUserPasswd ^ProxySOCKS4 proxy username password)
        :socks5 (.setUserPasswd ^ProxySOCKS5 proxy username password)))
    proxy))

(defn set-proxy
  [session proxy]
  (.setProxy
    ^Session (references/get-instance session)
    ^Proxy (make-proxy proxy)))

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
           remote-unix-socket
           remote-port
           connect-timeout]
    :or {bind-address "127.0.0.1"
         connect-timeout 0}}]
  (if remote-unix-socket
    (.setSocketForwardingL
      ^Session (references/get-instance session)
      ^String bind-address
      ^int local-port
      ^String remote-unix-socket
      nil
      ^int connect-timeout
      )
    (.setPortForwardingL
      ^Session (references/get-instance session)
      ^String bind-address
      ^int local-port
      ^String remote-host
      ^int remote-port
      nil
      ^int connect-timeout)))

(defn delete-port-forwarding-local
  [session
   {:keys [bind-address
           local-port]
    :or {bind-address "127.0.0.1"}}]
  (.delPortForwardingL
   ^Session (references/get-instance session)
   ^String bind-address
   ^int local-port))

(defn get-port-forwarding-local
  [session]
  (->>
   (.getPortForwardingL
    ^Session (references/get-instance session))
   (mapv (fn [s]
           (let [[local-port remote-host remote-port]
                 (string/split s #":")]
             (if (and (= remote-host "null")
                      (= remote-port "0"))
               ;; Jsch PortWatcher doesn't report socket forwarding path details
               {:local-port (Integer/parseInt local-port)}
               {:local-port (Integer/parseInt local-port)
                :remote-host remote-host
                :remote-port (Integer/parseInt remote-port)}))))))

(defn set-port-forwarding-remote
  [session
   {:keys [bind-address
           remote-port
           local-host
           local-port]
    :or {bind-address "127.0.0.1"
         local-host "127.0.0.1"}}]
  (.setPortForwardingR
   ^Session (references/get-instance session)
   ^String bind-address
   ^int remote-port
   ^String local-host
   ^int local-port))

(defn delete-port-forwarding-remote
  [session
   {:keys [bind-address
           remote-port]
    :or {bind-address "127.0.0.1"}}]
  (.delPortForwardingR
   ^Session (references/get-instance session)
   ^String bind-address
   ^int remote-port))

(defn get-port-forwarding-remote
  [session]
  (->>
   (.getPortForwardingR
    ^Session (references/get-instance session))
   (mapv (fn [s]
           (let [[local-port remote-host remote-port]
                 (string/split s #":")]
             {:remote-port (Integer/parseInt local-port)
              :local-host remote-host
              :local-port (Integer/parseInt remote-port)})))))

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
