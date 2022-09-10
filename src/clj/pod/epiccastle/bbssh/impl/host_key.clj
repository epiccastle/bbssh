(ns pod.epiccastle.bbssh.impl.host-key
  (:require [bbssh.impl.references :as references]
            [bbssh.impl.utils :as utils])
  (:import [com.jcraft.jsch HostKey]))

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
