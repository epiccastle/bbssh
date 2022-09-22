(ns pod.epiccastle.bbssh.impl.utils
  (:require [clojure.java.io :as io])
  (:import [java.nio.file Paths Files LinkOption Path FileSystems]
           [java.nio.file.attribute FileAttribute BasicFileAttributes BasicFileAttributeView
            PosixFilePermission PosixFilePermissions PosixFileAttributeView
            FileTime]))

;; pod.epiccastle.bbssh.impl.* are invoked on pod side.

(set! *warn-on-reflection* true)

(def empty-file-attribute-array
  (make-array FileAttribute 0))

(def empty-link-options
  (make-array LinkOption 0))

(def no-follow-links
  (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))

(defn last-access-time
  "return the last access time for the passed in file in seconds since the epoch.
  `file` is a string.
  "
  [file]
  (let [p (.toPath (io/file file))]
    (int
     (/ (.toMillis (.lastAccessTime (Files/readAttributes p java.nio.file.attribute.BasicFileAttributes ^"[Ljava.nio.file.LinkOption;" empty-link-options)))
        1000))))

(defn last-modified-time
  "return the last modified time for the passed in file in seconds since the epoch.
  `file` is a string.
  "
  [file]
  (let [p (.toPath (io/file file))]
    (int
     (/ (.toMillis (.lastModifiedTime (Files/readAttributes p java.nio.file.attribute.BasicFileAttributes ^"[Ljava.nio.file.LinkOption;" empty-link-options)))
        1000))))

(def permission->mode
  {PosixFilePermission/OWNER_READ     0400
   PosixFilePermission/OWNER_WRITE    0200
   PosixFilePermission/OWNER_EXECUTE  0100
   PosixFilePermission/GROUP_READ     0040
   PosixFilePermission/GROUP_WRITE    0020
   PosixFilePermission/GROUP_EXECUTE  0010
   PosixFilePermission/OTHERS_READ    0004
   PosixFilePermission/OTHERS_WRITE   0002
   PosixFilePermission/OTHERS_EXECUTE 0001})

(defn file-mode
  "returns the modification bits of the file as an integer. If you express this in octal
  you will get the representation chmod command uses. eg `(format \"%o\" (file-mode \".\"))` "
  [file]
  (let [p (.toPath (io/file file))
        perm-hash-set (.permissions ^java.nio.file.attribute.PosixFileAttributes (Files/readAttributes p java.nio.file.attribute.PosixFileAttributes ^"[Ljava.nio.file.LinkOption;" empty-link-options))]
    (reduce (fn [acc [perm-mode perm-val]]
              (if (.contains perm-hash-set perm-mode)
                (bit-or acc perm-val)
                acc))
            0 permission->mode)))
