(ns pod.epiccastle.bbssh.host-key
  (:require [pod.epiccastle.bbssh.pod.host-key :as host-key]
            [pod.epiccastle.bbssh.cleaner :as cleaner]
            [pod.epiccastle.bbssh.utils :as utils])
 )

(defn new
  "Create a new host-key with some subset of:

  - `host`: A string of the hostname.
  - `key`: A byte array of the public key.
  - `type`: One of `:unknown`, `:guess`, `:sshdss`, `:sshrsa`,
          `:ecdsa256`, `:ecdsa384`, `:ecdsa521`, `:ed25519` or `:ed448`.
          If uknown value passed, defaults to `:guess` which tries to
          guess the key type.
  - `comment`: A string comment for the key.
  - `marker`: A string marker for the key.
  "
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
  "Returns the hostname for the host-key."
  [host-key]
  (host-key/get-host
   (cleaner/split-key host-key)))

(defn get-type
  "Returns the key type string for the host-key.
  `\"ssh-rsa\"`, `\"ssh-dss\"`, `\"ecdsa-sha2-nistp256\"` or
  `\"ssh-ed25519\"`.
  "
  [host-key]
  (host-key/get-type
   (cleaner/split-key host-key)))

(defn get-key
  "Returns the key as a base64 encoded string."
  [host-key]
  (host-key/get-key
   (cleaner/split-key host-key)))

(defn get-finger-print
  "Returns the fingerprint of the key."
  [host-key agent]
  (host-key/get-finger-print
   (cleaner/split-key host-key)
   (cleaner/split-key agent)))

(defn get-comment
  "Returns the comment associated with the key."
  [host-key]
  (host-key/get-comment
   (cleaner/split-key host-key)))

(defn get-marker
  "Returns any @ marker associated with the key. If no marker is
  associated returns the empty string."
  [host-key]
  (host-key/get-marker
   (cleaner/split-key host-key)))

(defn get-info
  "Returns all the associated information of the key as a single hashmap
  with keys `:host`, `:type`, `:key`, `:finger-print`, `:comment` and
  `:marker`."
  [host-key agent]
  (host-key/get-info
   (cleaner/split-key host-key)
   (cleaner/split-key agent)))

(defn get-infos
  "Given a sequence of host-key references and the bbssh agent
  reference, return a hashmap of all the info for all the keys. The
  keys are the host-key references and the values are as would be
  returned from `get-info`."
  [host-keys agent]
  (->> (host-key/get-infos
        (mapv cleaner/split-key host-keys)
        (cleaner/split-key agent))
       (map (fn [[k v]]
              [(cleaner/register k) v]))
       (into {})))
