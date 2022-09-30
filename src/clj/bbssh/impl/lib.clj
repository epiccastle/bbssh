(ns bbssh.impl.lib
  (:require [clojure.java.io :as io]))

(def libs-set ["libbbssh.dylib" "libbbssh.so"])

(defn path-split
  "give a full path filename, return a tuple of
  [path basename]

  eg \"blog/posts/1/main.yml\" -> [\"blog/posts/1\" \"main.yml\"]
  "
  [filename]
  (let [file (io/file filename)]
    [(.getParent file) (.getName file)]))

(defn path-join
  "given multiple file path parts, join them all together with the
  file separator"
  [& parts]
  (.getPath (apply io/file parts)))

(defn setup
  "Copy any of the bundled dynamic libs from resources to the
  run time lib directory"
  [libs-dir]
  (doseq [filename libs-set]
    (when-let [file (io/resource filename)]
      (let [[_ name] (path-split (.getFile file))
            dest-path (path-join libs-dir name)
            resource-size (with-open [out (java.io.ByteArrayOutputStream.)]
                            (io/copy (io/input-stream file) out)
                            (count (.toByteArray out)))]
        ;; writing to a library while running its code can result in segfault
        ;; only write if filesize is different or it doesnt exist
        (when (or (not (.exists (io/file dest-path)))
                  (not= (.length (io/file dest-path)) resource-size))
          (io/copy (io/input-stream file) (io/file dest-path)))))))

(defn get-libs-dir
  "find out at runtime what the path of the libs directory is"
  []
  (let [home-dir (System/getenv "HOME")
        config-dir (path-join home-dir ".bbssh")
        libs-dir (path-join config-dir "libs")]
    libs-dir))

(defn native-image? []
  (and (= "Substrate VM" (System/getProperty "java.vm.name"))
       (= "runtime" (System/getProperty "org.graalvm.nativeimage.imagecode")))
  )

(defn init!
  "add the lib path to the java library path when running
  as a graalvm native image"
  []
  (let [libs-dir (get-libs-dir)]
    (.mkdirs (io/as-file libs-dir))
    (setup libs-dir)
    (when (native-image?)
      ;; most JVM implementations: this property needs to be set on launch
      ;; but graalvm allows us to dynamically change it
      (System/setProperty "java.library.path" libs-dir))))
