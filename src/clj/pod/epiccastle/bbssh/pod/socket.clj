(ns pod.epiccastle.bbssh.pod.socket
  (:refer-clojure :exclude [read])
  (:require [bbssh.impl.utils :as utils]))

(defn open [sock-path]
  (BbsshUtils/ssh-open-auth-socket sock-path)
  )

(defn close [sock-fd]
  (BbsshUtils/ssh-close-auth-socket sock-fd)
  )

(defn write [sock-fd base64]
  (let [buffer (utils/decode-base64 base64)]
    (BbsshUtils/ssh-auth-socket-write sock-fd buffer (count buffer))
    ))

(defn read [sock-fd size]
  (let [buffer (byte-array size)]
    (BbsshUtils/ssh-auth-socket-read sock-fd buffer size)
    (utils/encode-base64 buffer)))
