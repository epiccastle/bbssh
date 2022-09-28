(ns pod.epiccastle.bbssh.byte-array-input-stream
  "Creates and calls the various methods of a ByteArrayInputStream that
  exists on the pod heap."
  (:refer-clojure :exclude [flush read])
  (:require [pod.epiccastle.bbssh.pod.byte-array-input-stream :as byte-array-input-stream]
            [pod.epiccastle.bbssh.impl.cleaner :as cleaner]
            [pod.epiccastle.bbssh.impl.utils :as utils]))

(defn new
  [string-or-bytes & [encoding]]
  (cleaner/register
   (if (string? string-or-bytes)
     (byte-array-input-stream/new-from-string
      string-or-bytes encoding)
     (byte-array-input-stream/new-from-bytes
      (utils/encode-base64 string-or-bytes)))))

(defn available
  "Returns the number of remaining bytes that can be read (or skipped over) from this input stream."
  [stream]
  (byte-array-input-stream/available
   (cleaner/split-key stream)))

(defn close
  "Closing a ByteArrayInputStream has no effect."
  [stream]
  (byte-array-input-stream/close
   (cleaner/split-key stream)))

(defn mark
  "Set the current marked position in the stream."
  [stream read-ahead-limit]
  (byte-array-input-stream/mark
   (cleaner/split-key stream)
   read-ahead-limit))

(defn mark-supported
  "Tests if this InputStream supports mark/reset."
  [stream]
  (byte-array-input-stream/mark-supported
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
   (byte-array-input-stream/read
    (cleaner/split-key stream)))
  ([stream bytes offset length]
   (let [[bytes-read base64] (byte-array-input-stream/read
                              (cleaner/split-key stream)
                              length)]
     (when (pos? bytes-read)
       (System/arraycopy (utils/decode-base64 base64) 0 bytes offset bytes-read))
     bytes-read)))

(defn reset
  "Resets the buffer to the marked position."
  [stream]
  (byte-array-input-stream/reset
   (cleaner/split-key stream)))

(defn skip
  "Skips `n` bytes of input from this input stream."
  [stream n]
  (byte-array-input-stream/skip
   (cleaner/split-key stream)
   n))
