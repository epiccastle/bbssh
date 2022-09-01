(ns bb-test.docker
  (:require [babashka.pods :as pods]
            [babashka.process :as process]
            [babashka.wait :as wait]
            [clojure.string :as string])
  (:import [java.lang.ref WeakReference]))

(defn run [command error-message]
  (let [{:keys [exit err out]}
        (process/sh command)]
    (assert (zero? exit) (str error-message ": " err))
    out))

(defn run! [command]
  (process/sh command))

(defn build []
  (run
    "docker build -t bbssh/test-base test"
    "docker build failed"))

(defn cleanup []
  (run! "docker container stop bbssh-test")
  (run! "docker container rm bbssh-test")
  nil)

(defn start []
  (-> "docker run --name bbssh-test -d -p 8765:22 bbssh/test-base"
      (run "docker run failed")
      string/trim))
