(ns pod.epiccastle.bbssh.user-info
  (:require [pod.epiccastle.bbssh.utils :as utils]))

(defn new [callbacks]
  (utils/new-invoker
   {:call-sym 'pod.epiccastle.bbssh.impl.user-info/new
    :args []
    :callbacks callbacks}))
