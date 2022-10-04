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
        (if (System/getenv "TEST")
          ;; temporary tests for lib builds on different  archs
          (do
            (prn {:width (BbsshUtils/get-terminal-width)
                  :height (BbsshUtils/get-terminal-height)
                  :tty? (BbsshUtils/is-stdout-a-tty)})
            (comment
              (print "enter: ")
              (.flush *out*)
              (BbsshUtils/enter-raw-mode 1)
              (let [res (read-line)]
                (BbsshUtils/leave-raw-mode 1)
                (prn 'result res)))
            (when-let [auth-sock (System/getenv "SSH_AUTH_SOCK")
                       ]
              (let [auth-sock auth-sock
                    #_(if (string/starts-with? auth-sock "\\\\?\\")
                                (subs auth-sock 4)
                                auth-sock
                                )
                    _ (prn 'auth-sock auth-sock)
                    sock (BbsshUtils/ssh-open-auth-socket auth-sock)]
                (prn 'open sock)
                (let [send (byte-array [0 0 0 1 11])]
                  (prn 'send (seq send))
                  (BbsshUtils/ssh-auth-socket-write sock send (count send))
                  (let [read-buff (byte-array 4)]
                    (BbsshUtils/ssh-auth-socket-read sock read-buff 4)
                    (prn 'read (seq read-buff)))))))
          (pod/main))))))
