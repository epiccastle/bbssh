(ns bbssh.core
  (:require [bbssh.impl.lib :as lib]
            [bbssh.impl.terminal :as terminal]
            [bbssh.impl.userinfo :as userinfo]
            [bbssh.impl.session :as session]
            [bbssh.impl.pod :as pod])
  (:gen-class))

(defn -main [& args]
  (when-not (System/getenv "BABASHKA_POD")
    (binding [*out* *err*]
      (println "Error: bbssh needs to be run as a babashka pod."))
    (System/exit 1))

  (lib/init!)
  (clojure.lang.RT/loadLibrary "bbssh")
  (pod/main))
