(ns pod.epiccastle.bbssh.impl.input-stream
  (:refer-clojure :exclude [read])
  (:require [bbssh.impl.references :as references]
            [pod.epiccastle.bbssh.impl.callbacks :as callbacks]
            [pod.epiccastle.bbssh.impl.cleaner :as cleaner]
            [bbssh.impl.utils :as utils])
  (:import [java.io
            PipedInputStream PipedOutputStream
            ByteArrayInputStream ByteArrayOutputStream
            InputStream]
           [java.util Arrays]))

;; pod.epiccastle.bbssh.impl.* are invoked on pod side.

(set! *warn-on-reflection* true)

(defn new
  ([]
   (references/add-instance
    (PipedInputStream.)))
  ([src-or-pipe-size]
   (references/add-instance
    (if (int? src-or-pipe-size)
      (PipedInputStream.
       ^int src-or-pipe-size)
      (PipedInputStream.
       ^PipedOutputStream (references/get-instance src-or-pipe-size)))))
  ([src pipe-size]
   (references/add-instance
    (PipedInputStream.
     ^PipedOutputStream (references/get-instance src)
     ^int pipe-size))))

(defn close [stream]
  (.close
   ^PipedInputStream (references/get-instance stream)))

(defn ^:blocking read
  ([stream]
   (.read
    ^PipedInputStream (references/get-instance stream)))
  ([stream bytes]
   (let [arr (byte-array bytes)
         bytes-read
         (.read
          ^PipedInputStream (references/get-instance stream)
          arr
          0
          bytes)]
     [bytes-read
      (case bytes-read
        -1 nil
        0 ""
        (utils/encode-base64 (Arrays/copyOfRange arr 0 bytes-read)))])))

(defn available [stream]
  (.available
   ^PipedInputStream (references/get-instance stream)))

(defn connect [stream source]
  (.connect
   ^PipedInputStream (references/get-instance stream)
   ^PipedOutputStream (references/get-instance source)))

(defn ^:async new-pod-proxy
  [reply-fn]
  (let [result
        (references/add-instance
         (proxy [InputStream] []
           (available []
             (callbacks/call-method reply-fn :available []))
           (close []
             (callbacks/call-method reply-fn :close []))
           (mark [readlimit]
             (callbacks/call-method reply-fn :mark [readlimit]))
           (markSupported []
             (callbacks/call-method reply-fn :mark-supported []))
           (read
             ([]
              (callbacks/call-method reply-fn :read []))
             ([^bytes bytes]
              (let [[bytes-read base64]
                    (callbacks/call-method
                     reply-fn :read
                     [(count bytes)])
                    buffer (some-> base64 utils/decode-base64)]
                (when buffer
                  (System/arraycopy buffer 0 bytes 0 bytes-read))
                bytes-read))
             ([^bytes bytes offset length]
              (let [[bytes-read base64]
                    (callbacks/call-method
                     reply-fn :read
                     [length])
                    buffer (some-> base64 utils/decode-base64)]
                (when buffer
                  (System/arraycopy buffer 0 bytes offset bytes-read))
                bytes-read)))
           (reset []
             (callbacks/call-method reply-fn :reset []))
           (skip [n]
             (callbacks/call-method reply-fn :skip [n]))))]
    (cleaner/register-delete-fn result #(reply-fn [:done] ["done"]))
    (reply-fn [:result result])
    nil))
