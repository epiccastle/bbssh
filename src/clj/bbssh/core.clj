(ns bbssh.core
  (:require [bbssh.impl.lib :as lib])
  (:gen-class))

(declare init!)

(defn -main [& args]
  (lib/init!)
  (clojure.lang.RT/loadLibrary "bbssh")
  )
