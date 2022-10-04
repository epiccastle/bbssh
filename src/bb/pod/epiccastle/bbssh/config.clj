(ns pod.epiccastle.bbssh.config
  "Creates and calls the various methods of an ConfigRepository that
  exists on the pod heap."
  (:require [pod.epiccastle.bbssh.impl.cleaner :as cleaner]
            [pod.epiccastle.bbssh.impl.utils :as utils]))

(defn new
  "Create a new config instance. Pass in a hashmap containing
  the functions to execute as values. These functions will be
  called to gather information on the config. The hashmap should
  contain some subset of the following keywords:

  ```clojure
  :get-hostname (fn [] ...)
  ```
     return a string specifying the hostname in this config.

  ```clojure
  :get-user (fn [] ...)
  ```
     return a string specifying the username in this config.

  ```clojure
  :get-port (fn [] ...)
  ```
     return a number specifying the port in this config.

  ```clojure
  :get-value (fn [key] ...)
  ```
     return the string value in the config for the specified key.

  ```clojure
  :get-values (fn [] ...)
  ```
     return a vector of srtings in this config for the specified
  key."
  [callbacks]
  (utils/new-invoker
   {:call-sym 'pod.epiccastle.bbssh.pod.config/new
    :args []
    :callbacks callbacks}))

(defn get-hostname [config]
  (config/get-hostname
   (cleaner/split-key config)))

(defn get-user [config]
  (config/get-user
   (cleaner/split-key config)))

(defn get-port [config]
  (config/get-port
   (cleaner/split-key config)))

(defn get-value [config]
  (config/get-value
   (cleaner/split-key config)))

(defn get-values [config]
  (config/get-values
   (cleaner/split-key config)))
