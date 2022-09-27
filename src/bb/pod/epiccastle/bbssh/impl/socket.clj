(ns pod.epiccastle.bbssh.impl.socket
  (:refer-clojure :exclude [read])
  (:require [pod.epiccastle.bbssh.pod.socket :as socket]
            [pod.epiccastle.bbssh.impl.utils :as utils]))

(defn open [sock-path]
  (socket/open sock-path))

(defn close [sock-fd]
  (socket/close sock-fd))

(defn write [sock-fd bytes]
  (socket/write sock-fd (utils/encode-base64 bytes)))

(defn read [sock-fd size]
  (utils/decode-base64
   (socket/read sock-fd size)))
