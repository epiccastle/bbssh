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
         '[pod.epiccastle.bbssh.channel-exec :as channel-exec]
         '[pod.epiccastle.bbssh.input-stream :as input-stream]
         '[pod.epiccastle.bbssh.output-stream :as output-stream]
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

(defn streams-for-out []
  (let [os (output-stream/new)
        is (input-stream/new os 1024)]
    [os is]))

(defn streams-for-in []
  (let [is (input-stream/new 1024)
        os (output-stream/new is)]
    [is os]))

(defn foo []
  (let [a (agent/new)]
    (println "new returned:" a)
    (let [s (agent/get-session a (System/getenv "USER") "localhost" 22)]
      (println "get-session returned:" s)
      (prn (cleaner/get-references))
      (println "password:")
      (session/set-password s (read-line))
      (session/set-config s :strict-host-key-checking false)
      (session/connect s)
      (prn 'connected? (session/connected? s))
      (let [c (session/open-channel s "exec")

            is (input-stream/new)

            [out-stream out-in] (streams-for-out)
            [err-stream err-in] (streams-for-out)
            ]
        (channel-exec/set-command c "id")
        (channel-exec/set-input-stream c is false)
        (input-stream/close is)

        (channel-exec/set-output-stream c out-stream)
        (channel-exec/set-error-stream c err-stream)

        (channel-exec/connect c)
        (prn "reading...")
        (let [buff (byte-array 1024)
              num (input-stream/read out-in buff 0 1024)]
          (prn num)
          (prn (String. (java.util.Arrays/copyOfRange buff 0 num) "UTF-8")))))))

(foo)

(force-gc)
(Thread/sleep 1000)
(prn (cleaner/get-references))
