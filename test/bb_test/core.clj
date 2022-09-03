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
  (-> "clj -Spath -m bbssh.core"
      process/sh
      :out
      string/trim))

(case (System/getenv "BBSSH_TEST_TARGET")
  "native-image"
  ;; run pod process from native-image to test
  (pods/load-pod "./bbssh" {:transport :socket})

  ;; by default: run pod process under java in dev
  (pods/load-pod ["java"
                  "-Djava.library.path=resources"
                  "-cp"
                  class-path
                  "clojure.main"
                  "-m"
                  "bbssh.core"] {:transport :socket}))


(require '[bb-test.test-password-exec]
         '[bb-test.test-user-info]
         '[bb-test.test-garbage-collection])

(test/run-tests 'bb-test.test-password-exec
                'bb-test.test-user-info
                'bb-test.test-garbage-collection
                )
