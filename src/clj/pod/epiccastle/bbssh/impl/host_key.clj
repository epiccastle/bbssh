(ns pod.epiccastle.bbssh.impl.host-key
  (:require [bbssh.impl.references :as references]
            [bbssh.impl.utils :as utils])
  (:import [com.jcraft.jsch HostKey JSch]))

(def types
  {:unknown HostKey/UNKNOWN
   :guess HostKey/GUESS
   :sshdss HostKey/SSHDSS
   :sshrsa HostKey/SSHRSA
   :ecdsa256 HostKey/ECDSA256
   :ecdsa384 HostKey/ECDSA384
   :ecdsa521 HostKey/ECDSA521
   :ed25519 HostKey/ED25519
   :ed448 HostKey/ED448
   })

(defn new
  ([^String host ^bytes key]
   (references/add-instance
    (HostKey. host (utils/decode-base64 key))))
  ([^String host ^clojure.lang.Keyword type ^bytes key]
   (references/add-instance
    (HostKey. host (types type) (utils/decode-base64 key))))
  ([^String host ^clojure.lang.Keyword type ^bytes key ^String comment]
   (references/add-instance
    (HostKey. host (types type) (utils/decode-base64 key) comment)))
  ([^String marker ^String host ^clojure.lang.Keyword type ^bytes key ^String comment]
   (references/add-instance
    (HostKey. marker host (types type) (utils/decode-base64 key) comment))
   ))

(defn get-host
  [host-key]
  (.getHost
   ^HostKey (references/get-instance host-key)))

(defn get-type
  [host-key]
  (.getType
   ^HostKey (references/get-instance host-key)))

(defn get-key
  [host-key]
  (.getKey
   ^HostKey (references/get-instance host-key)))

(defn get-finger-print
  [host-key agent]
  (.getFingerPrint
   ^HostKey (references/get-instance host-key)
   ^JSch (references/get-instance agent)))

(defn get-comment
  [host-key]
  (.getComment
   ^HostKey (references/get-instance host-key)))

(defn get-marker
  [host-key]
  (.getMarker
   ^HostKey (references/get-instance host-key)))
