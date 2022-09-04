(ns pod.epiccastle.bbssh.utils)

(def decoder (java.util.Base64/getDecoder))

(defn decode-base64 [base64]
  (.decode decoder base64))

(def encoder (java.util.Base64/getEncoder))

(defn encode-base64 [array]
  (.encodeToString encoder array))
