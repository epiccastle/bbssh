(ns bbssh.impl.utils
  (:require [clojure.string :as string]))

(set! *warn-on-reflection* true)

(defn to-camel-case [^String a]
  (apply str (map string/capitalize (.split (name a) "-"))))

(defn string-to-byte-array [^String s]
  (byte-array (map int s)))
