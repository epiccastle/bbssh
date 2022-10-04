(ns pod.epiccastle.bbssh.config-repository
  "Creates and calls the various methods of an ConfigRepository that
  exists on the pod heap."
  (:require [pod.epiccastle.bbssh.impl.cleaner :as cleaner]
            [pod.epiccastle.bbssh.impl.utils :as utils]
            [pod.epiccastle.bbssh.pod.config-repository :as config-repository]))

(defn- postprocess-returns [method result]
  (case method
    :get-config
    (cleaner/split-key result)

    result))

(defn new
  "Create a new config-repository instance. Pass in a hashmap containing
  the function to execute as values. The hashmap should contain one
  keyword and value:

  ```clojure
  :get-config (fn [hostname] ...)
  ```
     return a config object to be used for the specified hostname.
  "
  [callbacks]
  (utils/new-invoker
   {:call-sym 'pod.epiccastle.bbssh.pod.config-repository/new
    :args []
    :callbacks callbacks
    :postprocess-returns-fn postprocess-returns}))

(defn get-config
  "return the config reference for the specified `hostname` in
  the `config-repository`"
  [config-repository hostname]
  (config-repository/get-config
   (cleaner/split-key config-repository)
   hostname))

(defn openssh-config-file
  "Create an OpenSSH config-repository from a file. `config-file`
  can be a string (supports tilde expansion of home directory)
  or a java.io.File instance."
  [config-file]
  (config-repository/openssh-config-file
   (if (string? config-file)
     config-file
     (.getPath config-file))))

(defn openssh-config-string
  "Create and OpenSSH config-repository from a data string."
  [config]
  (config-repository/openssh-config-string config))
