(ns pod.epiccastle.bbssh.pod.byte-array-input-stream
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

(defn new-from-string [^String string & [^String encoding]]
  (references/add-instance
   (ByteArrayInputStream.
    ^bytes (.getBytes string (or encoding "utf-8")))))

(defn new-from-bytes [^String string]
  (references/add-instance
   (ByteArrayInputStream.
    ^bytes (utils/decode-base64 string))))

(defn available [stream]
  (.available
   ^ByteArrayInputStream (references/get-instance stream)))

(defn close [stream]
  (.close
   ^ByteArrayInputStream (references/get-instance stream)))

(defn mark [stream read-ahead-limit]
  (.mark
   ^ByteArrayInputStream (references/get-instance stream)
   read-ahead-limit))

(defn mark-supported [stream]
  (.markSupported
   ^ByteArrayInputStream (references/get-instance stream)))

(defn ^:blocking read
  ([stream]
   (.read
    ^ByteArrayInputStream (references/get-instance stream)))
  ([stream bytes]
   (let [arr (byte-array bytes)
         bytes-read
         (.read
          ^ByteArrayInputStream (references/get-instance stream)
          arr
          0
          bytes)]
     [bytes-read
      (case bytes-read
        -1 nil
        0 ""
        (utils/encode-base64 (Arrays/copyOfRange arr 0 bytes-read)))])))

(defn reset [stream]
  (.reset
   ^ByteArrayInputStream (references/get-instance stream)))

(defn skip [stream n]
  (.skip
   ^ByteArrayInputStream (references/get-instance stream)
   n))
