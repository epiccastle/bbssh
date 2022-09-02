(ns pod.epiccastle.bbssh.cleaner
  (:require [pod.epiccastle.bbssh.impl.cleaner :as cleaner])
  (:import [java.lang.ref Cleaner]))

(def cleaner (Cleaner/create))

(defn split-key [key]
  [(namespace key) (name key)])

(defn register [key]
  (let [[namespace name] key

        ;; this is a transient keyword that can be garbage collected
        ref (keyword namespace name)]
    (.register
     cleaner ref
     (reify
       java.lang.Runnable
       (run [_]
         (cleaner/del-reference [namespace name]))))
    ref))
