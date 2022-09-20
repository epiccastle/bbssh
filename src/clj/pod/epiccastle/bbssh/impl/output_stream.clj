(ns pod.epiccastle.bbssh.impl.output-stream
  (:refer-clojure :exclude [flush])
  (:require [bbssh.impl.references :as references]
            [pod.epiccastle.bbssh.impl.callbacks :as callbacks]
            [pod.epiccastle.bbssh.impl.cleaner :as cleaner]
            [bbssh.impl.utils :as utils])
  (:import [java.io
            PipedOutputStream PipedInputStream
            OutputStream ByteArrayOutputStream]))

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

(defn byte-array-output-stream
  ([]
   (references/add-instance
    (ByteArrayOutputStream.)))
  ([size]
   (references/add-instance
    (ByteArrayOutputStream. size))))

(defn ^:async new-pod-proxy
  [reply-fn]
  (let [result
        (references/add-instance
         (proxy [OutputStream] []
           (close []
             (callbacks/call-method reply-fn :close []))
           (flush []
             (callbacks/call-method reply-fn :flush []))
           (write
             ([byte-array-or-number]
              (callbacks/call-method
               reply-fn :write
               [(if (number? byte-array-or-number)
                  byte-array-or-number
                  (utils/encode-base64 byte-array-or-number))]))
             ([^bytes byte-array offset length]
              (callbacks/call-method
               reply-fn :write
               [(utils/encode-base64
                 (java.util.Arrays/copyOfRange byte-array ^int offset ^int (+ offset length)))])))))]
    (cleaner/register-delete-fn result #(reply-fn [:done] ["done"]))
    (reply-fn [:result result])
    nil))
