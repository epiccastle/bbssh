(ns my-project.core
  (:require [babashka.pods :as pods]
            [babashka.process :as process]
            [clojure.string :as string]))

(def class-path
  (-> "clj -Spath -m bbssh.core"
      process/sh
      :out
      string/trim))

(println "loading pod..." )

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

(require
 '[pod.epiccastle.bbssh.user-info :as user-info]
 '[pod.epiccastle.bbssh.session :as session]
 '[pod.epiccastle.bbssh.agent :as agent]
 '[pod.epiccastle.bbssh.impl.cleaner :as cleaner]
 '[pod.epiccastle.bbssh.key-pair :as key-pair]
 )

(defn foo []
  (let [agent (agent/new)
        kp (key-pair/generate agent :rsa)]
    (prn (key-pair/get-finger-print kp))
    (key-pair/write-private-key kp "example.priv" (.getBytes "my-passphrase"))
    (key-pair/write-public-key kp "example.pub" "example comment")
    (prn (seq (key-pair/get-public-key-blob kp)))
    (prn (key-pair/get-key-size kp))
    ))

(foo)
