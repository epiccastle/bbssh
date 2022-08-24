(ns bbssh.core
  (:require [bbssh.impl.lib :as lib])
  (:gen-class))

(defn -main [& args]
  (when-not (System/getenv "BABASHKA_POD")
    (println "Error: bbssh needs to be run as a babashka pod.")
    (System/exit 1))

  (lib/init!)
  (clojure.lang.RT/loadLibrary "bbssh"))
