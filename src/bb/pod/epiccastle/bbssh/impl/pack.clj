(ns pod.epiccastle.bbssh.impl.pack)

(defn pack-byte [n]
  [(bit-and n 0xff)])

(defn pack-int [n]
  [(-> n (bit-shift-right 24) (bit-and 0xff))
   (-> n (bit-shift-right 16) (bit-and 0xff))
   (-> n (bit-shift-right 8) (bit-and 0xff))
   (-> n (bit-and 0xff))])

(defn pack-data [data]
  (let [len (count data)]
    (concat
     (pack-int len)
     data)))

(defn unpack-int [[b1 b2 b3 b4]]
  (bit-or
   (-> b1 (bit-and 0xff) (bit-shift-left 24))
   (-> b2 (bit-and 0xff) (bit-shift-left 16))
   (-> b3 (bit-and 0xff) (bit-shift-left 8))
   (-> b4 (bit-and 0xff))))

(defn decode-string
  "returns sequence (byte-stream remaining-data)"
  [data]
  (let [[v data] (split-at 4 data)]
    (split-at (unpack-int v) data)))
