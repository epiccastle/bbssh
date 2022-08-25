(ns bbssh.impl.pod
  (:refer-clojure :exclude [read-string read])
  (:require [bencode.core :refer [read-bencode write-bencode]])
  (:import [java.io PushbackInputStream]
           [java.net ServerSocket]))

(set! *warn-on-reflection* true)

(def debug? false)
(def debug-file "/tmp/bbssh-pod-debug.txt")

(defn read-string [^"[B" v]
  (String. v))

(defmacro debug [& args]
  (if debug?
    `(with-open [wrtr# (io/writer debug-file :append true)]
       (.write wrtr# (prn-str ~@args)))
    nil))

(def stdin (PushbackInputStream. System/in))

(defn write [out v]
  (write-bencode out v))

(defn read [in]
  (read-bencode in))

(defn safe-read [d]
  (when d
    (read-string d)))
