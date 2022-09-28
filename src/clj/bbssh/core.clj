(ns bbssh.core
  (:require [bbssh.impl.lib :as lib]
            [bbssh.impl.pod :as pod]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.tools.cli :as cli])
  (:gen-class))

(def version
  (-> "BBSSH_VERSION"
      io/resource
      slurp
      string/trim))

(defn -main [& args]
  (let [{:keys [options]} (cli/parse-opts args [["-v" "--version"]])]
    (if (:version options)
      (println "bbssh version" version)
      (do
        (when-not (System/getenv "BABASHKA_POD")
          (binding [*out* *err*]
            (println "Error: bbssh needs to be run as a babashka pod."))
          (System/exit 1))

        (lib/init!)
        (clojure.lang.RT/loadLibrary "bbssh")
        (pod/main)))))
