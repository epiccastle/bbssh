(ns pod.epiccastle.bbssh.cleaner
  "Handle garbage collection of the pod instances.

  > __Note:__ This namespace is an implementation detail and is not
  needed unless you are extending `bbssh` itself.

  Keywords are not passed across the pod boundary as they interfere
  with garbage collection. Instead the pod and the client interface
  with values that are a vector of two strings: A string representing
  the namespace or the java package and a string representing the name
  or class name.

  The client recieves a transient keyword constructed by `register`.
  When this passed out of scope it is garbage collected by babashka
  and this triggers a deletion of the instance it refers to from the
  pod's heap.

  Before sending this keyword as a reference to the pod `invoke`, use
  `split-key` to translate it to the reference vector used by the pod.
  "
  (:require [pod.epiccastle.bbssh.pod.cleaner :as cleaner])
  (:import [java.lang.ref Cleaner]))

(def
  ^{:doc "The `java.lang.ref.Cleaner` instance used to manage garbage collection."}
  cleaner
  (Cleaner/create))

(defn split-key
  "Split a fully qualified keyword into a vector of
  two strings, the namespace, and the name.
  "
  [key]
  [(namespace key) (name key)])

(defn register
  "Register a key (a vector of namespace and name) returned from
  the pod `invoke` to garbage collected. Returns the keyword to be used
  inside babashka code.
  "
  [key]
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
