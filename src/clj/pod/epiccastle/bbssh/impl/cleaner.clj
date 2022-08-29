(ns pod.epiccastle.bbssh.impl.cleaner
  (:require [bbssh.impl.references :as references]))

(defn del-reference [key]
  (references/delete-key key)
  nil)

(defn get-references []
  (-> @references/references
      :key->instance
      keys))
