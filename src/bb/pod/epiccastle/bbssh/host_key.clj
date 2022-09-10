(ns pod.epiccastle.bbssh.host-key
  (:require [pod.epiccastle.bbssh.impl.host-key :as host-key]
            [pod.epiccastle.bbssh.utils :as utils])
 )

(defn new
  "Create a new host-key."
  ([^String host ^bytes key]
   (host-key/new host (utils/encode-base64 key)))
  ([^String host ^clojure.lang.Keyword type ^bytes key]
   (host-key/new host type (utils/encode-base64 key)))
  ([^String host ^clojure.lang.Keyword type ^bytes key ^String comment]
   (host-key/new host type (utils/encode-base64 key) comment))
  ([^String marker ^String host ^clojure.lang.Keyword type ^bytes key ^String comment]
   (host-key/new marker host type (utils/encode-base64 key) comment)))
