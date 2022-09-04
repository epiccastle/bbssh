(ns pod.epiccastle.bbssh.key-pair
  (:require [pod.epiccastle.bbssh.impl.key-pair :as key-pair]
            [pod.epiccastle.bbssh.cleaner :as cleaner]
            [pod.epiccastle.bbssh.utils :as utils]))

(defn generate
  ([agent key-type]
   (generate agent key-type 2048))
  ([agent key-type key-size]
   (cleaner/register
    (key-pair/generate
     (cleaner/split-key agent)
     key-type
     key-size))))

(defn set-passphrase [key-pair passphrase]
  (key-pair/set-passphrase
   (cleaner/split-key key-pair)
   passphrase))

(defn write-private-key
  ([key-pair filename]
   (key-pair/write-private-key
    (cleaner/split-key key-pair)
    filename))
  ([key-pair filename passphrase]
   (key-pair/write-private-key
    (cleaner/split-key key-pair)
    filename
    (utils/encode-base64 passphrase))))

(defn write-public-key [key-pair filename comment]
  (key-pair/write-public-key
   (cleaner/split-key key-pair)
   filename
   comment))

(defn get-finger-print [key-pair]
  (key-pair/get-finger-print
   (cleaner/split-key key-pair)))

(defn get-public-key-blob [key-pair]
  (utils/decode-base64
   (key-pair/get-public-key-blob
    (cleaner/split-key key-pair))))

(defn get-key-size [key-pair]
  (key-pair/get-key-size
   (cleaner/split-key key-pair)))
