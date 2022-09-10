(ns pod.epiccastle.bbssh.host-key
  (:require [pod.epiccastle.bbssh.impl.host-key :as host-key]
            [pod.epiccastle.bbssh.cleaner :as cleaner]
            [pod.epiccastle.bbssh.utils :as utils])
 )

(defn new
  "Create a new host-key."
  ([^String host ^bytes key]
   (cleaner/register
    (host-key/new host (utils/encode-base64 key))))
  ([^String host ^clojure.lang.Keyword type ^bytes key]
   (cleaner/register
    (host-key/new host type (utils/encode-base64 key))))
  ([^String host ^clojure.lang.Keyword type ^bytes key ^String comment]
   (cleaner/register
    (host-key/new host type (utils/encode-base64 key) comment)))
  ([^String marker ^String host ^clojure.lang.Keyword type ^bytes key ^String comment]
   (cleaner/register
    (host-key/new marker host type (utils/encode-base64 key) comment))))

(defn get-host
  [host-key]
  (host-key/get-host
   (cleaner/split-key host-key)))

(defn get-type
  [host-key]
  (host-key/get-type
   (cleaner/split-key host-key)))

(defn get-key
  [host-key]
  (host-key/get-key
   (cleaner/split-key host-key)))

(defn get-finger-print
  [host-key agent]
  (host-key/get-finger-print
   (cleaner/split-key host-key)
   (cleaner/split-key agent)
   ))

(defn get-comment
  [host-key]
  (host-key/get-comment
   (cleaner/split-key host-key)))

(defn get-marker
  [host-key]
  (host-key/get-marker
   (cleaner/split-key host-key)))
