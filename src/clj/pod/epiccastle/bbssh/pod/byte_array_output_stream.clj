(ns pod.epiccastle.bbssh.pod.byte-array-output-stream
  (:refer-clojure :exclude [flush])
  (:require [bbssh.impl.references :as references]
            [pod.epiccastle.bbssh.pod.callbacks :as callbacks]
            [pod.epiccastle.bbssh.pod.cleaner :as cleaner]
            [bbssh.impl.utils :as utils])
  (:import [java.io
            ByteArrayOutputStream OutputStream]))

;; pod.epiccastle.bbssh.pod.* are invoked on pod side.

(set! *warn-on-reflection* true)

(defn new
  ([]
   (references/add-instance
    (ByteArrayOutputStream.)))
  ([size]
   (references/add-instance
    (ByteArrayOutputStream. size))))

(defn close [stream]
  (.close
   ^ByteArrayOutputStream (references/get-instance stream)))

(defn reset [stream]
  (.reset
   ^ByteArrayOutputStream (references/get-instance stream)))

(defn size [stream]
  (.size
   ^ByteArrayOutputStream (references/get-instance stream)))

(defn to-byte-array [stream]
  (utils/encode-base64
   (.toByteArray
    ^ByteArrayOutputStream (references/get-instance stream))))

(defn to-string
  ([stream]
   (.toString
    ^ByteArrayOutputStream (references/get-instance stream)))
  ([stream encoding]
   (.toString
    ^ByteArrayOutputStream (references/get-instance stream)
    ^String encoding)))

(defn write
  ([stream int-or-base64]
   (if (string? int-or-base64)
     (let [buffer (utils/decode-base64 int-or-base64)
           size (count buffer)]
       (.write
        ^ByteArrayOutputStream (references/get-instance stream)
        ^bytes buffer
        0
        size))
     (.write
      ^ByteArrayOutputStream (references/get-instance stream)
      ^int int-or-base64))))

(defn write-to
  [stream out]
  (.writeTo
   ^ByteArrayOutputStream (references/get-instance stream)
   ^OutputStream (references/get-instance out)))
