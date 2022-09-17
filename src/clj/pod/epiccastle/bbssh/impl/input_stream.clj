(ns pod.epiccastle.bbssh.impl.input-stream
  (:refer-clojure :exclude [read])
  (:require [bbssh.impl.references :as references]
            [bbssh.impl.utils :as utils])
  (:import [java.io PipedInputStream PipedOutputStream]
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

(defn read
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
