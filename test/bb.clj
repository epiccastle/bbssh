(ns my-project.core
  (:require [babashka.pods :as pods]
            [babashka.process :as process]
            [clojure.string :as string]))

(def class-path
  (-> "clj -Spath -m bbssh.core"
      process/sh
      :out
      string/trim))

(println "loading..." )

;; run pod process under java in dev
(pods/load-pod ["java"
                "-Djava.library.path=resources"
                "-cp"
                class-path
                "clojure.main"
                "-m"
                "bbssh.core"] {:transport :socket})

;; run pod process from native-image to test
#_(pods/load-pod "./bbssh" {:transport :socket})

(println "loaded")
