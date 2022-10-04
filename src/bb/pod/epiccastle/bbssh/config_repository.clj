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

(defn get-config [config-repository hostname]
  (config-repository/get-config
   (cleaner/split-key config-repository)
   hostname))
