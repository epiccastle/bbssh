(ns bb-test.test-scp
  (:require [pod.epiccastle.bbssh.core :as bbssh]
            [pod.epiccastle.bbssh.scp :as scp]
            [babashka.process :as process]
            [bb-test.docker :as docker]
            [clojure.test :refer [is deftest]]
            [clojure.java.io :as io]
            [clj-commons.digest :as digest])
  (:import [java.io File BufferedInputStream]))

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
    (is (= (docker/exec "sh -c 'cd /root/bbssh-test && find files -exec file {} \\; | sort -r'")
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

    (is (= (docker/exec "sh -c 'cd /root/bbssh-test && find files -exec md5sum {} \\; | sort'")
           "0f343b0931126a20f133d67c2b018a3b  files/dir2/zeroes
67287b8ef38d90cfeef66729c8d32e39  files/dir2/random
d41d8cd98f00b204e9800998ecf8427e  files/dir1/zero
d8e8fca2dc0f896fd7cb4cb0031ba249  files/dir2/test.txt
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
      (:out (process/sh "bash -c 'cd test && find files/ -type f -exec md5sum {} \\; | sort'"))
      (:out (process/sh "bash -c 'cd .tmp && find files/ -type f -exec md5sum {} \\; | sort'"))))

    (is
     (=
      (:out (process/sh "bash -c 'cd test && find files/ -exec file {} \\; | sort'"))
      (:out (process/sh "bash -c 'cd .tmp && find files/ -exec file {} \\; | sort'")))))
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
    ))

(deftest test-scp-to-progress
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})

  (let [session (bbssh/ssh "localhost" {:port 9876
                                        :username "root"
                                        :password "root-access-please"
                                        :strict-host-key-checking false})

        progress-state (atom [])
        ]
    (scp/scp-to [(io/file "deps.edn")] "bbssh-test"
                {:session session})
    (is (= (docker/md5 "/root/bbssh-test")
           (digest/md5 (io/as-file "deps.edn"))))

    (docker/exec "rm /root/bbssh-test")
    (docker/exec "mkdir /root/bbssh-test")
    (is
     (= 10
        (scp/scp-to [(io/file "test/files")
                     ["content🚀" {:name "string"}]
                     [(byte-array [1 2 3 4]) {:name "byte-array"}]
                     [(io/input-stream (byte-array [0xf0 0x9f 0x9a 0x80 0x00]))
                      {:name "input-stream"
                       :size 5
                       }]]
                    "bbssh-test"
                    {:session session
                     :recurse? true
                     :buffer-size 500
                     :progress-context 0
                     :progress-fn (fn [context data]
                                    (let [{:keys [source offset size]} data
                                          source-data
                                          (cond
                                            (= File (class source))
                                            (.getPath source)

                                            (bytes? source)
                                            (seq source)

                                            (= BufferedInputStream (class source))
                                            :input-stream

                                            :else
                                            source)]
                                      (swap!
                                       progress-state
                                       conj
                                       [context source-data offset size])
                                      (inc context)))})))
    (is (= (docker/exec "od -t x1 /root/bbssh-test/byte-array")
           "0000000 01 02 03 04
0000004
"))
    (is (= "6c746063e72ea0391871ee3916a6c41c"
           (docker/md5 "/root/bbssh-test/string")))
    (is (= "26daa5c2fe95de841884180225c6d6da"
           (docker/md5 "/root/bbssh-test/input-stream")))
    (is (= (docker/exec "sh -c 'cd /root/bbssh-test && find files -exec file {} \\; | sort -r'")
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

    (is (= (docker/exec "sh -c 'cd /root/bbssh-test && find files -exec md5sum {} \\; | sort'")
           "0f343b0931126a20f133d67c2b018a3b  files/dir2/zeroes
67287b8ef38d90cfeef66729c8d32e39  files/dir2/random
d41d8cd98f00b204e9800998ecf8427e  files/dir1/zero
d8e8fca2dc0f896fd7cb4cb0031ba249  files/dir2/test.txt
edfcbda2f87663507ecf63eeb885b956  files/.hidden
"))

    ;; non determinative
    #_(is
     (= @progress-state
        [[0 "test/files/dir2/zeroes" 500 1024]
         [1 "test/files/dir2/zeroes" 1000 1024]
         [2 "test/files/dir2/zeroes" 1024 1024]
         [3 "test/files/dir2/test.txt" 5 5]
         [4 "test/files/dir2/random" 115 115]
         [5 "test/files/dir1/zero" 0 0]
         [6 "test/files/.hidden" 11 11]
         [7 "content🚀" 11 11]
         [8 '(1 2 3 4) 4 4]
         [9 :input-stream 5 5]])))

  (docker/cleanup)
  )


(deftest test-scp-from-progress
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})

  (let [session (bbssh/ssh "localhost" {:port 9876
                                        :username "root"
                                        :password "root-access-please"
                                        :strict-host-key-checking false})
        progress-state (atom [])]
    (docker/cp-to "test/files/dir2/random" "/root/random")

    (scp/scp-from "random" "/tmp/random"
                  {:session session})

    (is (= (digest/md5 (io/as-file "/tmp/random"))
           (docker/md5 "/root/random")))


    (docker/put-dir "test" "files" "/root/")
    (process/sh "rm -rf .tmp")
    (process/sh "mkdir .tmp")
    (is
     (= 7
        (scp/scp-from "/root/files" ".tmp"
                      {:session session
                       :recurse? true
                       :buffer-size 500
                       :progress-context 0
                       :progress-fn (fn [context data]
                                      (let [{:keys [dest offset size]} data]
                                        (swap! progress-state
                                               conj [context (.getPath dest) offset size]))
                                      (inc context))
                       })))
    (is
     (=
      (:out (process/sh "bash -c 'cd test && find files/ -type f -exec md5sum {} \\;'"))
      (:out (process/sh "bash -c 'cd .tmp && find files/ -type f -exec md5sum {} \\;'"))))

    (is
     (=
      (:out (process/sh "bash -c 'cd test && find files/ -exec file {} \\;'"))
      (:out (process/sh "bash -c 'cd .tmp && find files/ -exec file {} \\;'"))))

    ;; non determinative
    #_(is
     (=
      @progress-state
      [[0 ".tmp/files/dir2/zeroes" 500 1024]
       [1 ".tmp/files/dir2/zeroes" 1000 1024]
       [2 ".tmp/files/dir2/zeroes" 1024 1024]
       [3 ".tmp/files/dir2/test.txt" 5 5]
       [4 ".tmp/files/dir2/random" 115 115]
       [5 ".tmp/files/dir1/zero" 0 0]
       [6 ".tmp/files/.hidden" 11 11]])))

  (docker/cleanup))
