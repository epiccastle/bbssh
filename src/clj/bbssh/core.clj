(ns bbssh.core
  (:require [bbssh.impl.pod :as pod]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.tools.cli :as cli])
  (:gen-class))

(def version
  (-> "BBSSH_VERSION"
      io/resource
      slurp
      string/trim))

(defn native-image? []
  (and (= "Substrate VM" (System/getProperty "java.vm.name"))
       (= "runtime" (System/getProperty "org.graalvm.nativeimage.imagecode"))))

(defn -main [& args]
  (let [{:keys [options]} (cli/parse-opts args [["-v" "--version"]])]
    (if (:version options)
      (println "bbssh version" version)
      (do
        (when-not (System/getenv "BABASHKA_POD")
          (binding [*out* *err*]
            (println "Error: bbssh needs to be run as a babashka pod."))
          (System/exit 1))
        (when-not (native-image?)
          (clojure.lang.RT/loadLibrary "bbssh"))
        (pod/main))))
  (shutdown-agents))
