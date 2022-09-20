(ns pod.epiccastle.bbssh.byte-array-output-stream
  (:refer-clojure :exclude [flush])
  (:require [pod.epiccastle.bbssh.impl.byte-array-output-stream :as byte-array-output-stream]
            [pod.epiccastle.bbssh.cleaner :as cleaner]
            [pod.epiccastle.bbssh.utils :as utils]))

(defn new
  ([]
   (cleaner/register
    (byte-array-output-stream/new)))
  ([size]
   (cleaner/register
    (byte-array-output-stream/new))))

(defn close
  "Closing a ByteArrayOutputStream has no effect."
  [stream]
  (byte-array-output-stream/close
   (cleaner/split-key stream)))

(defn reset
  "Resets the count field of this byte array output stream to zero, so
  that all currently accumulated output in the output stream is
  discarded."
  [stream]
  (byte-array-output-stream/reset
   (cleaner/split-key stream)))

(defn size
  "Returns the current size of the buffer."
  [stream]
  (byte-array-output-stream/size
   (cleaner/split-key stream)))

(defn to-byte-array
  "creates a newly allocated byte-array containing the data and returns
  it."
  [stream]
  (utils/decode-base64
   (byte-array-output-stream/to-byte-array
    (cleaner/split-key stream))))

(defn to-string
  "Converts the buffer's contents into a string decoding bytes using the
  platform's default character set if encoding is not specified, else
  uses encoding."
  ([stream]
   (byte-array-output-stream/to-string
    (cleaner/split-key stream)))
  ([stream encoding]
   (byte-array-output-stream/to-string
    (cleaner/split-key stream)
    encoding)))

(defn write
  "write a byte or bytes to the output stream."
  ([stream byte]
   (byte-array-output-stream/write
    (cleaner/split-key stream)
    byte))
  ([stream bytes offset length]
   (byte-array-output-stream/write
    (cleaner/split-key stream)
    (utils/encode-base64
     (java.util.Arrays/copyOfRange bytes offset (+ offset length))))))

(defn write-to
  "Writes the complete contents of this byte array output stream to the
  specified output stream argument."
  ([stream out]
   (byte-array-output-stream/write-to
    (cleaner/split-key stream)
    (cleaner/split-key out))))
