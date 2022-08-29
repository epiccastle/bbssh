(ns my-project.core
  (:require [babashka.pods :as pods]
            [babashka.process :as process]
            [clojure.string :as string])
  (:import [java.lang.ref WeakReference]))

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

(require '[pod.epiccastle.bbssh.agent :as agent]
         '[pod.epiccastle.bbssh.session :as session]
         '[pod.epiccastle.bbssh.impl.cleaner :as cleaner])


(defn gc []
  (println "gc")
  (System/gc)
  (System/runFinalization)
  (println "gc done")
  )

(defn force-gc []
  (println "force-gc")
  (let [t (atom (Object.))
        wr (WeakReference. @t)]
    (reset! t nil)
    (while (.get wr)
      (System/gc)
      (System/runFinalization)))
  (println "force-gc done"))

(defn foo []
  (prn 1)
  (let [a (agent/new)]
    (prn 2)
    (println "new returned:" a)
    (force-gc)
    (force-gc)
    (let [s (agent/get-session a "crispin" "localhost" 22)]
      (println "get-session returned:" s)
      (force-gc)
      (prn (cleaner/get-references))
      )
    ))

(foo)
(force-gc)
(force-gc)
(Thread/sleep 1000)
(prn (cleaner/get-references))
