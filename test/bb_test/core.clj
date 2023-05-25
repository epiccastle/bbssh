(ns bb-test.core
  (:require [babashka.pods :as pods]
            [babashka.process :as process]
            [babashka.wait :as wait]
            [clojure.string :as string]
            [clojure.test :as test]
            [bb-test.docker :as docker]
            )
  (:import [java.lang.ref WeakReference]))

(def class-path
  (-> "clojure -Spath -m bbssh.core"
      process/sh
      :out
      string/trim
      (str ":src/c/jni")))

(case (System/getenv "BBSSH_TEST_TARGET")
  "native-image"
  ;; run pod process from native-image to test
  (pods/load-pod "build/bbssh" {:transport :socket})

  ;; by default: run pod process under java in dev
  (pods/load-pod ["java"
                  "-Djava.library.path=build"
                  "-cp"
                  class-path
                  "clojure.main"
                  "-m"
                  "bbssh.core"] {:transport :socket}))


(require '[bb-test.test-utils]
         '[bb-test.test-ssh-identity]
         '[bb-test.test-scp]
         '[bb-test.test-password-exec]
         '[bb-test.test-user-info]
         '[bb-test.test-garbage-collection]
         '[bb-test.test-key-pair]
         '[bb-test.test-identity-repository]
         '[bb-test.test-host-key]
         '[bb-test.test-known-hosts]
         '[bb-test.test-openssh-config]
         '[bb-test.test-port-forward]
         '[bb-test.test-proxy-support])

(defn -main [& args]
  (let [result
        (test/run-tests
         'bb-test.test-ssh-identity
         'bb-test.test-scp
         'bb-test.test-utils
         'bb-test.test-password-exec
         'bb-test.test-user-info
         'bb-test.test-garbage-collection
         'bb-test.test-key-pair
         'bb-test.test-identity-repository
         'bb-test.test-host-key
         'bb-test.test-known-hosts
         'bb-test.test-openssh-config
         'bb-test.test-port-forward
         'bb-test.test-proxy-support)]
    (prn result)
    (when (or
           (pos? (:fail result))
           (pos? (:error result)))
      (System/exit 1))))
