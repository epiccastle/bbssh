(ns bbssh.impl.utils
  (:require [clojure.string :as string])
  (:import [java.util Base64 Base64$Encoder Base64$Decoder]))

(set! *warn-on-reflection* true)

(defn to-camel-case [^String a]
  (apply str (map string/capitalize (.split (name a) "-"))))

#_ (to-camel-case "one-two-three")
#_ (to-camel-case "one")

(defn string-to-byte-array [^String s]
  (byte-array (map int s)))

(defn boolean-to-yes-no [val]
  (if (boolean? val)
    (if val "yes" "no")
    val))

(def base64-encoder (Base64/getEncoder))

(defn encode-base64 [^bytes to-encode]
  (.encodeToString ^Base64$Encoder base64-encoder to-encode))

(def base64-decoder (Base64/getDecoder))

(defn decode-base64 [^String to-decode]
  (.decode ^Base64$Decoder base64-decoder to-decode))
