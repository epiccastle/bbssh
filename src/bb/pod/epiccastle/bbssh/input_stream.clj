(ns pod.epiccastle.bbssh.input-stream
  (:refer-clojure :exclude [read])
  (:require [pod.epiccastle.bbssh.impl.input-stream :as input-stream]
            [pod.epiccastle.bbssh.cleaner :as cleaner]
            [pod.epiccastle.bbssh.utils :as utils]))

(defn new
  "Create a new PipedInputStream on the pod heap. Return
  a reference to it for babashka use."
  ([]
   (cleaner/register
    (input-stream/new)))
  ([src-or-pipe-size]
   (cleaner/register
    (input-stream/new
     (if (int? src-or-pipe-size)
       src-or-pipe-size
       (cleaner/split-key src-or-pipe-size)))))
  ([src pipe-size]
   (cleaner/register
    (input-stream/new
     (cleaner/split-key src)
     pipe-size))))

(defn close
  "Close the stream"
  [stream]
  (input-stream/close
   (cleaner/split-key stream)))

(defn read
  "`(read stream)`
  Read a single byte from the stream. Returns an int. Blocks
  if a byte is not available.

  `(read stream byte-array offset length)`
  Try and read `length` bytes from the `stream` and store them into
  a `byte-array` starting at `offset`. Returns the number of bytes
  successfully read. Does not block.
  "
  ([stream]
   (input-stream/read
    (cleaner/split-key stream)))
  ([stream bytes offset length]
   (let [[bytes-read base64] (input-stream/read
                              (cleaner/split-key stream)
                              length)]
     (when (pos? bytes-read)
       (System/arraycopy (utils/decode-base64 base64) 0 bytes offset bytes-read))
     bytes-read)))

(defn available
  "Return the number of bytes available and waiting to be read
  immediately in the stream"
  [stream]
  (input-stream/available
   (cleaner/split-key stream)))

(defn connect
  "Connect a bbssh PipedOutputStream to this stream."
  [stream source]
  (input-stream/connect
   (cleaner/split-key stream)
   (cleaner/split-key source)))

(defn byte-array-input-stream
  [string]
  (cleaner/register
   (input-stream/byte-array-input-stream
    string)))

(defn make-proxy
  "Make a babashka java.io.PipedInputStream that calls
  the pod heap input-stream `stream`."
  [stream]
  (proxy [java.io.PipedInputStream] []
    (close []
      (close stream))
    (read
      ([]
       (read stream))
      ([bytes offset length]
       (read stream bytes offset length)))
    (available []
      (available stream))))
