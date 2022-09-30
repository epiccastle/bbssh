(ns build
  "Builds GraalVM native images from deps.edn projects."
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.deps.alpha :as deps]
            [clojure.tools.namespace.find :refer [find-namespaces-in-dir]])
  (:import (java.io BufferedReader File)))

(defn native-image-classpath
  "Returns the current tools.deps classpath string, minus clj.native-image and plus *compile-path*."
  []
  (as-> (System/getProperty "java.class.path") $
        (string/split $ (re-pattern (str File/pathSeparatorChar)))
        (remove #(string/includes? "clj.native-image" %) $) ;; exclude ourselves
        (cons *compile-path* $) ;; prepend compile path for classes
        (string/join File/pathSeparatorChar $)))

(def windows? (string/starts-with? (System/getProperty "os.name") "Windows"))

(defn merged-deps
  "Merges install, user, local deps.edn maps left-to-right."
  []
  (let [{:keys [install-edn user-edn project-edn]} (deps/find-edn-maps)]
    (deps/merge-edns [install-edn user-edn project-edn])))

(defn sh
  "Launches a process with optional args, returning exit code.
  Prints stdout & stderr."
  [bin & args]
  (let [arg-array ^"[Ljava.lang.String;" (into-array String (cons bin args))
        process (-> (ProcessBuilder. arg-array)
                    (.redirectErrorStream true) ;; TODO stream stderr to stderr
                    (.start))]
    (with-open [out (io/reader (.getInputStream process))]
      (loop []
        (when-let [line (.readLine ^BufferedReader out)]
          (println line)
          (recur))))
    (.waitFor process)))

(defn exec-native-image
  "Executes native-image (bin) with opts, specifying a classpath,
   main/entrypoint class, and destination path."
  [bin opts cp main]
  (let [cli-args (cond-> []
                   (seq opts)     (into opts)
                   cp             (into ["-cp" cp])
                   main           (conj main)
                   ;; apparently native-image --no-server isn't currently supported on Windows
                   (not windows?) (conj "--no-server"))]
    (apply sh bin cli-args)))

(defn prep-compile-path []
  (let [compile-path (io/file *compile-path*)]
    (doseq [file (-> compile-path (file-seq) (rest) (reverse))]
      (io/delete-file file))
    (.mkdir compile-path)))

(defn native-image-bin-path []
  (let [graal-paths [(str (System/getenv "GRAALVM_HOME") "/bin")
                     (System/getenv "GRAALVM_HOME")]
        paths (lazy-cat graal-paths (string/split (System/getenv "PATH") (re-pattern (File/pathSeparator))))
        filename (cond-> "native-image" windows? (str ".cmd"))]
    (first
     (for [path (distinct paths)
           :let [file (io/file path filename)]
           :when (.exists file)]
       (.getAbsolutePath file)))))

(defn- munge-class-name [class-name]
  (string/replace class-name "-" "_"))

(defn build [main-ns opts]
  (let [[nat-img-path & nat-img-opts]
        (if (some-> (first opts) (io/file) (.exists)) ;; check first arg is file path
          opts
          (cons (native-image-bin-path) opts))]
    (when-not nat-img-path
      (binding [*out* *err*]
        (println "Could not find GraalVM's native-image!")
        (println "Please make sure that the environment variable $GRAALVM_HOME is set")
        (println "The native-image tool must also be installed ($GRAALVM_HOME/bin/gu install native-image)")
        (println "If you do not wish to set the GRAALVM_HOME environment variable,")
        (println "you may pass the path to native-image as the second argument to clj.native-image"))
      (System/exit 1))
    (when-not (string? main-ns)
      (binding [*out* *err*] (println "Main namespace required e.g. \"script\" if main file is ./script.clj"))
      (System/exit 1))

    (let [deps-map (merged-deps)
          namespaces (mapcat (comp find-namespaces-in-dir io/file) (:paths deps-map))]
      (prep-compile-path)
      (doseq [ns (distinct (cons main-ns namespaces))]
        (println "Compiling" ns)
        (compile (symbol ns)))

      (System/exit
       (exec-native-image
        nat-img-path
        nat-img-opts
        (native-image-classpath)
        (munge-class-name main-ns))))))

(def project-root (.getAbsolutePath (File. "")))

(defn -main [& extra-args]
  (try
    (build
     "bbssh.core"
     (->>
      (concat
       [
        "--enable-https"
        "--no-fallback"
        "--allow-incomplete-classpath"
        "--enable-all-security-services"
        "-H:+ReportExceptionStackTraces"
        "-H:ConfigurationFileDirectories=graal-configs/"
        "--initialize-at-build-time"
        "--initialize-at-run-time=com.jcraft.jsch.PortWatcher"
        "--native-compiler-options=-I{{PROJECT_ROOT}}/src/c"
        "--native-compiler-options=-L{{PROJECT_ROOT}}/build"
        "-H:Name=bbssh"]
       extra-args)
      (map (fn [arg]
             (if (.contains arg "{{PROJECT_ROOT}}")
               (string/replace arg #"\{\{PROJECT_ROOT\}\}" project-root)
               arg)))))
    (finally
      (shutdown-agents))))
