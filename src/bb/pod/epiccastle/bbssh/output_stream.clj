(ns pod.epiccastle.bbssh.output-stream
  (:refer-clojure :exclude [flush])
  (:require [pod.epiccastle.bbssh.impl.output-stream :as output-stream]
            [pod.epiccastle.bbssh.cleaner :as cleaner]
            [pod.epiccastle.bbssh.utils :as utils]))

(defn new
  "Create a new PipedOutputStream.
  Optional first argument can be a bbssh PipedInputStream to
  connect as the sink.
  "
  ([]
   (cleaner/register
    (output-stream/new)))
  ([sink]
   (cleaner/register
    (output-stream/new
     (cleaner/split-key sink)))))

(defn close
  "Close the stream"
  [stream]
  (output-stream/close
   (cleaner/split-key stream)))

(defn write
  "`(write stream byte)`
  Write a single byte (int) to the stream.

  `(write stream byte-array offset length)`
  Write `length` bytes from `byte-array` beginning at `offset`
  to `stream`.
  "
  ([stream byte]
   (output-stream/write
    (cleaner/split-key stream)
    byte))
  ([stream byte-array offset length]
   (output-stream/write
    (cleaner/split-key stream)
    (utils/encode-base64
     (java.util.Arrays/copyOfRange byte-array offset length))
    length)))

(defn connect
  "Connect a bbssh PipedInputStream to this to act as a sink"
  [stream sink]
  (output-stream/connect
   (cleaner/split-key stream)
   (cleaner/split-key sink)))

(defn flush
  "Flush the stream"
  [stream]
  (output-stream/flush
   (cleaner/split-key stream)))
