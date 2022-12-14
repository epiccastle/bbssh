(ns pod.epiccastle.bbssh.output-stream
  "Creates and calls the various methods of a PipedOutputStream that
  exists on the pod heap."
  (:refer-clojure :exclude [flush])
  (:require [pod.epiccastle.bbssh.pod.output-stream :as output-stream]
            [pod.epiccastle.bbssh.impl.cleaner :as cleaner]
            [pod.epiccastle.bbssh.impl.utils :as utils]))

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
  "`(write stream bytes)`
  Write a byte-array `bytes` to the stream.

  `(write stream byte-array offset length)`
  Write `length` bytes from `byte-array` beginning at `offset`
  to `stream`.
  "
  ([stream bytes]
   (output-stream/write
    (cleaner/split-key stream)
    (utils/encode-base64 bytes)))
  ([stream byte-array offset length]
   (output-stream/write
    (cleaner/split-key stream)
    (utils/encode-base64
     (java.util.Arrays/copyOfRange byte-array offset (+ offset length)))
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

(defn make-proxy
  "Make a babashka java.io.PipedOutputStream that calls the pod heap
  output-stream `stream`."
  [stream]
  (proxy [java.io.PipedOutputStream] []
    (close []
      (close stream))
    (write
      ([bytes]
       (write stream bytes))
      ([byte-array offset length]
       (write stream byte-array offset length)))
    (connect [sink]
      (connect stream sink))
    (flush []
      (flush stream))))

(defn- preprocess-args [method args]
  (case method
    :write
    (let [[base64-or-int] args]
      [(if (string? base64-or-int)
         (utils/decode-base64 base64-or-int)
         base64-or-int)])

    args))

(defn new-pod-proxy
  [callbacks]
  (utils/new-invoker
   {:call-sym 'pod.epiccastle.bbssh.pod.output-stream/new-pod-proxy
    :args []
    :callbacks callbacks
    :preprocess-args-fn preprocess-args}))
