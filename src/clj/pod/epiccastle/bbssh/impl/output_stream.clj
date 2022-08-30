(ns pod.epiccastle.bbssh.impl.output-stream
  (:refer-clojure :exclude [flush])
  (:require [bbssh.impl.references :as references]
            [bbssh.impl.utils :as utils])
  (:import [java.io PipedOutputStream PipedInputStream]))

;; pod.epiccastle.bbssh.impl.* are invoked on pod side.

(set! *warn-on-reflection* true)

(defn new
  ([]
   (references/add-instance
    (PipedOutputStream.)))
  ([sink]
   (references/add-instance
    (PipedOutputStream. (references/get-instance sink)))))

(defn close [stream]
  (.close
   ^PipedOutputStream (references/get-instance stream)))

(defn write
  ([stream byte]
   (.write
    ^PipedOutputStream (references/get-instance stream)
    ^int byte))
  ([stream base64 _length]
   (let [arr (utils/decode-base64 base64)]
     (.write
      ^PipedOutputStream (references/get-instance stream)
      ^bytes arr
      0
      (count arr)))))

(defn connect [stream sink]
  (.connect
   ^PipedOutputStream (references/get-instance stream)
   ^PipedInputStream (references/get-instance sink)))

(defn flush [stream]
  (.flush
   ^PipedOutputStream (references/get-instance stream)))
