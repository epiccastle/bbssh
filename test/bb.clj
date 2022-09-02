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
 )

(defn foo []
  (let [ui (user-info/new
            {:get-password (fn []
                             (prn :get-password)
                             "incorrect-password")
             :prompt-yes-no (fn [s]
                              (prn :prompt-yes-no s)
                              true)
             :get-passphrase (fn []
                               (prn :get-passphrase)
                               "passphrase")
             :prompt-passphrase (fn [s]
                                  (prn :prompt-passphrase s)
                                  true)
             :prompt-password (fn [s]
                                (prn :prompt-password s)
                                true)
             :show-message (fn [s]
                             (prn :show-message s))})

        agent (agent/new)
        session (agent/get-session agent
                                   (System/getenv "USER")
                                   "localhost"
                                   22)
        ]
    (session/set-config session :strict-host-key-checking false)
    (session/set-user-info session ui)
    (try (session/connect session)
         (catch Exception e))

    )
  )

(foo)
(prn "running gc...")
(System/gc)
(Thread/sleep 1000)
(prn "all cleaned?" (empty? (cleaner/get-references)))
(Thread/sleep 1000)
