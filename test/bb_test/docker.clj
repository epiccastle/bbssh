(ns bb-test.docker
  (:require [babashka.pods :as pods]
            [babashka.process :as process]
            [babashka.wait :as wait]
            [clojure.string :as string])
  (:import [java.lang.ref WeakReference]))

(defn run [command error-message]
  (let [{:keys [exit err out]}
        (process/sh command)]
    (assert (zero? exit) (str error-message ": out:" out " err:" err))
    out))

(defn run! [command]
  (process/sh command))

(defn build [{:keys [root-password]}]
  (run
    (format
     "docker build -t bbssh/test-base --build-arg root_password=%s test"
     root-password)
    "docker build failed"))

(defn cleanup []
  (run! "docker container stop bbssh-test")
  (run! "docker container rm bbssh-test")
  nil)

(defn start [{:keys [ssh-port]}]
  (-> "docker run --name bbssh-test -d -p %d:22 bbssh/test-base"
      (format ssh-port)
      (run "docker run failed")
      string/trim))

(defn stop []
  (run! "docker container stop bbssh-test"))

(defn exec [command]
  (run
    (str "docker exec bbssh-test " command)
    "docker exec failed"))

(defn exec! [command]
  (run!
    (str "docker exec bbssh-test " command)))

(defn cp-to [local-src remote-dest]
  (run
    (format "docker cp \"%s\" \"bbssh-test:%s\"" local-src remote-dest)
    "docker cp failed"))

(defn cp-from [remote-src local-dest]
  (run
    (format "docker cp \"bbssh-test:%s\" \"%s\"" remote-src local-dest)
    "docker cp failed"))

(defn put-file [contents remote-dest]
  (process/sh
   ["docker" "exec" "bbssh-test" "ash" "-c"
    (format "echo '%s' > '%s'"
            contents
            remote-dest)]))

(defn put-dir
  "transfer a complete local directory to the docker container"
  [src-dir src-path dest-path]
  (process/sh "rm /tmp/bbssh-tarball.tgz")
  (process/sh (format "tar -cvz -C '%s' -f /tmp/bbssh-tarball.tgz '%s'" src-dir src-path))
  (exec "rm -f /tmp/bbssh-tarball.tgz")
  (cp-to "/tmp/bbssh-tarball.tgz" "/tmp/bbssh-tarball.tgz")
  (exec
   (format "tar -xv -f /tmp/bbssh-tarball.tgz -C '%s'"
           dest-path)))

(defn md5 [path]
  (-> (exec (format "md5sum '%s'" path))
      (string/split #" ")
      first))

(defn get-container-ip
  []
  (-> (process/sh
        "docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' bbssh-test")
      :out
      (string/trim)))
