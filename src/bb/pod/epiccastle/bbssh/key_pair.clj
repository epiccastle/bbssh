(ns pod.epiccastle.bbssh.key-pair
  (:require [pod.epiccastle.bbssh.impl.key-pair :as key-pair]
            [pod.epiccastle.bbssh.cleaner :as cleaner]))

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
   (cleaner/split-key agent)
   passphrase))

(defn write-private-key [key-pair filename]
  (key-pair/write-private-key
   (cleaner/split-key agent)
   filename))

(defn write-public-key [key-pair filename comment]
  (key-pair/write-public-key
   (cleaner/split-key agent)
   filename
   comment))
