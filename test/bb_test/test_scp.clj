(ns bb-test.test-scp
  (:require [pod.epiccastle.bbssh.core :as bbssh]
            [pod.epiccastle.bbssh.agent :as agent]
            [pod.epiccastle.bbssh.session :as session]
            [pod.epiccastle.bbssh.scp :as scp]
            [pod.epiccastle.bbssh.channel-exec :as channel-exec]
            [pod.epiccastle.bbssh.input-stream :as input-stream]
            [pod.epiccastle.bbssh.output-stream :as output-stream]
            [babashka.process :as process]
            [bb-test.docker :as docker]
            [clojure.test :refer [is deftest]]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [clj-commons.digest :as digest]))

(deftest test-scp-to
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})

  (let [session (bbssh/ssh "localhost" {:port 9876
                                        :username "root"
                                        :password "root-access-please"
                                        :strict-host-key-checking false})]
    (scp/scp-to [(io/file "deps.edn")] "bbssh-test"
                {:session session})
    (is (= (docker/md5 "/root/bbssh-test")
           (digest/md5 (io/as-file "deps.edn"))))

    (docker/exec "rm /root/bbssh-test")
    (docker/exec "mkdir /root/bbssh-test")
    (scp/scp-to [(io/file "test/files")
                 ["content🚀" {:name "string"}]
                 [(byte-array [1 2 3 4]) {:name "byte-array"}]
                 [(io/input-stream (byte-array [0xf0 0x9f 0x9a 0x80 0x00]))
                  {:name "input-stream"
                   :size 5
                   }]]
                "bbssh-test"
                {:session session
                 :recurse? true})
    (is (= (docker/exec "od -t x1 /root/bbssh-test/byte-array")
           "0000000 01 02 03 04
0000004
"))
    (is (= "6c746063e72ea0391871ee3916a6c41c"
           (docker/md5 "/root/bbssh-test/string")))
    (is (= "26daa5c2fe95de841884180225c6d6da"
           (docker/md5 "/root/bbssh-test/input-stream")))
    (is (= (docker/exec "sh -c 'cd /root/bbssh-test && find files -exec file {} \\;'")
           "files: directory
files/dir2: directory
files/dir2/zeroes: data
files/dir2/test.txt: ASCII text
files/dir2/random: data
files/dir1: directory
files/dir1/zero: empty
files/dir1/dir3: directory
files/.hidden: Unicode text, UTF-8 text
"))

    (is (= (docker/exec "sh -c 'cd /root/bbssh-test && find files -exec md5sum {} \\;'")
           "0f343b0931126a20f133d67c2b018a3b  files/dir2/zeroes
d8e8fca2dc0f896fd7cb4cb0031ba249  files/dir2/test.txt
67287b8ef38d90cfeef66729c8d32e39  files/dir2/random
d41d8cd98f00b204e9800998ecf8427e  files/dir1/zero
edfcbda2f87663507ecf63eeb885b956  files/.hidden
")))

  (docker/cleanup))

(deftest test-scp-from
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})

  (let [session (bbssh/ssh "localhost" {:port 9876
                                        :username "root"
                                        :password "root-access-please"
                                        :strict-host-key-checking false})]
    (docker/cp-to "test/files/dir2/random" "/root/random")

    (scp/scp-from "random" "/tmp/random" {:session session})

    (is (= (digest/md5 (io/as-file "/tmp/random"))
           (docker/md5 "/root/random")))


    (docker/put-dir "test" "files" "/root/")
    (process/sh "rm -rf .tmp")
    (process/sh "mkdir .tmp")
    (scp/scp-from "/root/files" ".tmp"
                  {:session session
                   :recurse? true
                   })
    (is
     (=
      (:out (process/sh "bash -c 'cd test && find files/ -type f -exec md5sum {} \\;'"))
      (:out (process/sh "bash -c 'cd .tmp && find files/ -type f -exec md5sum {} \\;'"))))

    (is
     (=
      (:out (process/sh "bash -c 'cd test && find files/ -exec file {} \\;'"))
      (:out (process/sh "bash -c 'cd .tmp && find files/ -exec file {} \\;'")))))

  (docker/cleanup))

(deftest test-scp-no-recurse-error
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})

  (let [session (bbssh/ssh "localhost" {:port 9876
                                        :username "root"
                                        :password "root-access-please"
                                        :strict-host-key-checking false})]
    (process/sh "rm -rf .tmp")
    (process/sh "mkdir .tmp")
    (docker/put-dir "test" "files" "/root/")
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"not a regular file"
                 (scp/scp-from "/root/files" ".tmp"
                               {:session session})))
    )
  )
