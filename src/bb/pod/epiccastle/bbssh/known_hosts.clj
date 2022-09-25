(ns pod.epiccastle.bbssh.known-hosts
  (:require [pod.epiccastle.bbssh.pod.known-hosts :as known-hosts]
            [pod.epiccastle.bbssh.cleaner :as cleaner]
            [pod.epiccastle.bbssh.utils :as utils])
 )

;; (defn new
;;   "Create a new known-hosts."
;;   [agent]
;;   (cleaner/register
;;    (known-hosts/new
;;     (cleaner/split-key agent))))
