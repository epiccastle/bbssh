(ns bb-test.test-utils
  (:require [pod.epiccastle.bbssh.impl.utils :as utils]
            [babashka.process :as process]
            [clojure.test :refer [is deftest]]
            [clojure.string :as string]
            [clojure.java.io :as io])
  (:import [java.nio.file.attribute PosixFilePermission])
  )

(deftest test-permission-set->mode
  (is (= 0644
         (utils/permission-set->mode
          #{PosixFilePermission/OWNER_READ
            PosixFilePermission/OWNER_WRITE
            PosixFilePermission/GROUP_READ
            PosixFilePermission/OTHERS_READ})))
  (is (= 0755
         (utils/permission-set->mode
          #{PosixFilePermission/OWNER_READ
            PosixFilePermission/OWNER_WRITE
            PosixFilePermission/OWNER_EXECUTE
            PosixFilePermission/GROUP_READ
            PosixFilePermission/GROUP_EXECUTE
            PosixFilePermission/OTHERS_READ
            PosixFilePermission/OTHERS_EXECUTE
            })))
  (is (= 0777
         (utils/permission-set->mode
          #{PosixFilePermission/OWNER_READ
            PosixFilePermission/OWNER_WRITE
            PosixFilePermission/OWNER_EXECUTE
            PosixFilePermission/GROUP_READ
            PosixFilePermission/GROUP_WRITE
            PosixFilePermission/GROUP_EXECUTE
            PosixFilePermission/OTHERS_READ
            PosixFilePermission/OTHERS_WRITE
            PosixFilePermission/OTHERS_EXECUTE
            })))
  (is (= 0
         (utils/permission-set->mode
          #{}))))

(deftest test-mode->permission-set
  (is (= (utils/mode->permission-set 0644)
         #{PosixFilePermission/OWNER_READ
           PosixFilePermission/OWNER_WRITE
           PosixFilePermission/GROUP_READ
           PosixFilePermission/OTHERS_READ}))
  (is (= (utils/mode->permission-set 0755)
         #{PosixFilePermission/OWNER_READ
           PosixFilePermission/OWNER_WRITE
           PosixFilePermission/OWNER_EXECUTE
           PosixFilePermission/GROUP_READ
           PosixFilePermission/GROUP_EXECUTE
           PosixFilePermission/OTHERS_READ
           PosixFilePermission/OTHERS_EXECUTE
           }))
  (is (= (utils/mode->permission-set 0777)
         #{PosixFilePermission/OWNER_READ
           PosixFilePermission/OWNER_WRITE
           PosixFilePermission/OWNER_EXECUTE
           PosixFilePermission/GROUP_READ
           PosixFilePermission/GROUP_WRITE
           PosixFilePermission/GROUP_EXECUTE
           PosixFilePermission/OTHERS_READ
           PosixFilePermission/OTHERS_WRITE
           PosixFilePermission/OTHERS_EXECUTE
           }))
  (is (= (utils/mode->permission-set 0)
         #{})))

(deftest test-create-file
  (process/sh "rm -rf .test")
  (process/sh "mkdir .test")
  (process/sh "rm -f .test/bbssh-test-file")
  (utils/create-file (io/file ".test/bbssh-test-file") 0644)
  (is (-> (:out (process/sh "ls -l .test/bbssh-test-file"))
          (string/starts-with? "-rw-r--r--")))
  (process/sh "rm -f .test/bbssh-test-file")
  (utils/create-file (io/file ".test/bbssh-test-file") 0755)
  (is (-> (:out (process/sh "ls -l .test/bbssh-test-file"))
          (string/starts-with? "-rwxr-xr-x")))
  (process/sh "rm -f .test/bbssh-test-file")
  (utils/create-file (io/file ".test/bbssh-test-file") 0000)
  (is (-> (:out (process/sh "ls -l .test/bbssh-test-file"))
          (string/starts-with? "----------")))
  (process/sh "rm -f .test/bbssh-test-file")
  (utils/create-file (io/file ".test/bbssh-test-file") 0777)
  (is (-> (:out (process/sh "ls -l .test/bbssh-test-file"))
          (string/starts-with? "-rwxrwxrwx")))
  (is (thrown? java.nio.file.FileAlreadyExistsException
               (utils/create-file
                (io/file ".test/bbssh-test-file")
                0777)))
  (process/sh "rm -f .test/bbssh-test-file")
  (process/sh "rm -rf .test"))

(deftest test-create-directory
  (process/sh "rm -rf .test")
  (process/sh "mkdir .test")
  (utils/create-dirs (io/file ".test/bbssh-test-dir") 0755)
  (is (-> (process/sh "ls -al .test/bbssh-test-dir")
           :out
           string/split-lines
           second
           (string/starts-with? "drwxr-xr-x")))

  (process/sh "rm -rf .test/bbssh-test-dir")
  (utils/create-dirs (io/file ".test/bbssh-test-dir") 0700)
  (is (-> (process/sh "ls -al .test/bbssh-test-dir")
           :out
           string/split-lines
           second
           (string/starts-with? "drwx------")))

  (process/sh "rm -rf .test/bbssh-test-dir")
  (utils/create-dirs (io/file ".test/bbssh-test-dir") 0701)
  (is (-> (process/sh "ls -al .test/bbssh-test-dir")
           :out
           string/split-lines
           second
           (string/starts-with? "drwx-----x")))

  (process/sh "rm -rf .test/bbssh-test-dir")
  (utils/create-dirs (io/file ".test/bbssh-test-dir/deeper") 0700)
  (is (-> (process/sh "ls -al .test/bbssh-test-dir")
           :out
           string/split-lines
           second
           (string/starts-with? "drwx------")))
  (is (-> (process/sh "ls -al .test/bbssh-test-dir/deeper")
           :out
           string/split-lines
           second
           (string/starts-with? "drwx------")))
  (process/sh "rm -rf .test")
  )
