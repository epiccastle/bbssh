(ns pod.epiccastle.bbssh.host-key-repository
  (:refer-clojure :exclude [remove])
  (:require [pod.epiccastle.bbssh.impl.host-key-repository :as host-key-repository]
            [pod.epiccastle.bbssh.cleaner :as cleaner]
            [pod.epiccastle.bbssh.utils :as utils]))

(defn- preprocess-args [method args]
  (case method
    :check
    (let [[host key] args]
      [host (utils/decode-base64 key)])

    :add
    (let [[host-key user-info] args]
      [(cleaner/register host-key)
       (cleaner/register user-info)])

    :remove
    (let [[host type key] args]
      (if key
        [host type (utils/decode-base64 key)]
        [host key]))

    args))

(defn- postprocess-returns [method result]
  (case method
    :get-host-key
    (mapv cleaner/split-key result)

    result))

(defn new
  "Create a new host-key-repository. Pass in a hashmap containing
  callbacks to be used for the methods of the repository. The hashmap
  keys are optional. If a value is not specified a default is used.
  The keys are as follows:

  ```clojure
  :check (fn [^String host ^bytes public-key] ...)
  ```

    Check the repository for the presence of the passed in public
    key under the specified hostname. Hosts should be able to
    have multiple keys of different key types. Thus you should
    examine the key type and check that hosts keys for the same
    type. Your function should return `:ok` if the key is present
    and matches, `:not-included` if no matching key is found and
    `:changed` if there is a conflicting key of that type stored
    against the hostname.

  ```clojure
  :add (fn [^Keyword host-key ^Keyword user-info] ...)
  ```

    Add the referenced `host-key` into the repository. If the key
    requires user interaction use the passed in `user-info` to
    do so.

  ```clojure
  :remove (fn
            ([^String host ^String type] ..)
            ([^String host ^String type ^bytes public-key] ..)
  ```

    Remove the referenced `public-key` of `type` being stored
    for `host`. If `public-key` is not passed, remove all keys
    of `type` for `host`. `type` will be one of SSH public key
    prefix strings `\"ssh-rsa\"`, `\"ssh-dss\"`,
    `\"ecdsa-sha2-nistp256\"` or `\"ssh-ed25519\"`. Other key
    types may be added with later releases and so should be
    accomodated.

  "
  [callbacks]
  (utils/new-invoker
   {:call-sym 'pod.epiccastle.bbssh.impl.host-key-repository/new
    :args []
    :callbacks callbacks
    :preprocess-args-fn preprocess-args
    :postprocess-returns-fn postprocess-returns}))

(defn check [host-key-repository host key]
  (host-key-repository/check
   (cleaner/split-key host-key-repository)
   host
   (utils/encode-base64 key)))

(defn add [host-key-repository host-key user-info]
  (host-key-repository/add
   (cleaner/split-key host-key-repository)
   (cleaner/split-key host-key)
   (cleaner/split-key user-info)))

(defn remove
  ([host-key-repository host type]
   (host-key-repository/remove
    (cleaner/split-key host-key-repository)
    host
    type))
  ([host-key-repository host type key]
   (host-key-repository/remove
    (cleaner/split-key host-key-repository)
    host
    type
    (utils/encode-base64 key))))

(defn get-host-key
  ([host-key-repository]
   (mapv cleaner/register
         (host-key-repository/get-host-key
          (cleaner/split-key host-key-repository))))
  ([host-key-repository host type]
   (mapv cleaner/register
         (host-key-repository/get-host-key
          (cleaner/split-key host-key-repository)
          host type))))

(defn get-known-hosts-repository-id [host-key-repository]
  (host-key-repository/get-known-hosts-repository-id
   (cleaner/split-key host-key-repository)))
