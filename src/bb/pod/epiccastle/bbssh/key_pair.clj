(ns pod.epiccastle.bbssh.key-pair
  "Everything related to SSH public/private key pairs. Functions for
  generating, loading, saving, encrypting and decrypting keys. Also
  a function for signing arbitrary data with the private key.
  "
  (:refer-clojure :exclude [load])
  (:require [pod.epiccastle.bbssh.pod.key-pair :as key-pair]
            [pod.epiccastle.bbssh.impl.cleaner :as cleaner]
            [pod.epiccastle.bbssh.impl.utils :as utils]))

(defn generate
  "Generate a public/private SSH key pair.
  `key-type` should be `:dsa`, `:rsa`, `:ecdsa`, `:ed25519`
  or `:ed448`.
  `key-size` is the number of bits and defaults to 2048.
  "
  ([agent key-type]
   (generate agent key-type 2048))
  ([agent key-type key-size]
   (cleaner/register
    (key-pair/generate
     (cleaner/split-key agent)
     key-type
     key-size))))

(defn set-passphrase
  "Set the passphrase on the private key to the string
  `passphrase`"
  [key-pair passphrase]
  (key-pair/set-passphrase
   (cleaner/split-key key-pair)
   passphrase))

(defn write-private-key
  "write the private key to a file `filename`. Optionally
  pass in a byte array `passphrase` to be used as a passphrase."
  ([key-pair filename]
   (key-pair/write-private-key
    (cleaner/split-key key-pair)
    filename))
  ([key-pair filename passphrase]
   (key-pair/write-private-key
    (cleaner/split-key key-pair)
    filename
    (utils/encode-base64 passphrase))))

(defn write-public-key
  "write the public key to file `filename` with the attached
  `comment` string."
  [key-pair filename comment]
  (key-pair/write-public-key
   (cleaner/split-key key-pair)
   filename
   comment))

(defn get-finger-print
  "return the key finger print as a string."
  [key-pair]
  (key-pair/get-finger-print
   (cleaner/split-key key-pair)))

(defn get-public-key-blob
  "returns a byte-array of the raw public key data."
  [key-pair]
  (utils/decode-base64
   (key-pair/get-public-key-blob
    (cleaner/split-key key-pair))))

(defn get-key-size
  "returns the bit length of the key"
  [key-pair]
  (key-pair/get-key-size
   (cleaner/split-key key-pair)))

(defn dispose
  "zero out the memory holding the private key passphrase
  so subsequent attacks on stale memory are thwarted"
  [key-pair]
  (key-pair/dispose
   (cleaner/split-key key-pair)))

(defn is-encrypted
  "returns true if the private key is encrypted with a
  passphrase"
  [key-pair]
  (key-pair/is-encrypted
   (cleaner/split-key key-pair)))

(defn decrypt
  "decrypt the private key with the passed in byte-array
  so that the private key is no longer stored encrypted.
  Can be followed up with setting a new passphrase to
  re-encrypt. Returns true if the decryption succeeded."
  [key-pair passphrase]
  (key-pair/decrypt
   (cleaner/split-key key-pair)
   (utils/encode-base64 passphrase)))

(defn load
  "Load the key pair from a file. Pass both private and
  public filenames in to load from those files. If public
  key filename is omitted, the private key filename with
  \".pub\" appended is used"
  ([agent private-key-file]
   (load agent private-key-file (str private-key-file ".pub")))
  ([agent private-key-file public-key-file]
   (cleaner/register
    (key-pair/load
     (cleaner/split-key agent)
     private-key-file
     public-key-file))))

(defn load-bytes
  "Load the key pair from a byte array. Pass both private and
  public keys either as byte arrays or as strings. Half a keypair
  can be loaded to perform some operations. You may pass in
  `nil` for one of the key portions to only load the public or private
  portion."
  [agent private-key-bytes public-key-bytes]
  (cleaner/register
    (key-pair/load-bytes
      (cleaner/split-key agent)
      (when private-key-bytes
        (if (bytes? private-key-bytes)
          (utils/encode-base64 private-key-bytes)
          (utils/encode-base64 (.getBytes private-key-bytes))))
      (when public-key-bytes
        (if (bytes? public-key-bytes)
          (utils/encode-base64 public-key-bytes)
          (utils/encode-base64 (.getBytes public-key-bytes)))))))

(defn get-signature
  "Sign the passed in data with the private key, using algorithm
  if it is passed aswell"
  ([key-pair data]
   (utils/decode-base64
    (key-pair/get-signature
     (cleaner/split-key key-pair)
     (utils/encode-base64 data))))
  ([key-pair data algorithm]
   (utils/decode-base64
    (key-pair/get-signature
     (cleaner/split-key key-pair)
     (utils/encode-base64 data)
     algorithm))))
