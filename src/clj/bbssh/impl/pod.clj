(ns bbssh.impl.pod
  (:refer-clojure :exclude [read-string read])
  (:require [bbssh.impl.lookup :as lookup]
            [pod.epiccastle.bbssh.impl.core]
            [pod.epiccastle.bbssh.impl.cleaner]
            [pod.epiccastle.bbssh.impl.agent]
            [pod.epiccastle.bbssh.impl.session]
            [pod.epiccastle.bbssh.impl.channel-exec]
            [pod.epiccastle.bbssh.impl.input-stream]
            [pod.epiccastle.bbssh.impl.output-stream]
            [pod.epiccastle.bbssh.core]
            [pod.epiccastle.bbssh.cleaner]
            [pod.epiccastle.bbssh.agent]
            [pod.epiccastle.bbssh.session]
            [pod.epiccastle.bbssh.channel-exec]
            [pod.epiccastle.bbssh.input-stream]
            [pod.epiccastle.bbssh.output-stream]
            [bencode.core :refer [read-bencode write-bencode]]
            [clojure.edn :as edn]
            [clojure.string :as string]
            [clojure.java.io :as io])
  (:import [java.io PushbackInputStream]
           [java.net ServerSocket]))

(set! *warn-on-reflection* true)

(def debug? true)
(def debug-file "/tmp/bbssh-pod-debug.txt")

(defn read-string [^"[B" v]
  (String. v))

(defmacro debug [& args]
  (if debug?
    `(with-open [wrtr# (io/writer debug-file :append true)]
       (.write wrtr# (prn-str ~@args)))
    nil))

(def stdin (PushbackInputStream. System/in))

(defn write [out v]
  (write-bencode out v))

(defn read [in]
  (read-bencode in))

(defn safe-read [d]
  (when d
    (read-string d)))

(defmacro ns-public-describes [namespaces]
  (into []
        (for [namespace namespaces]
          {"name" (str namespace)
           "vars" (into []
                        (for [[n var] (ns-publics namespace)]
                          {"name" (name n)}))})))

#_
(macroexpand-1
 '(ns-public-describes [pod.epiccastle.bbssh.impl.core
                        pod.epiccastle.bbssh.impl.cleaner
                        ]))

(defmacro ns-public-slurps [namespaces]
  (into []
        (for [namespace namespaces]
          {"name" (str namespace)
           "vars" [{"name" "_"
                     "code" (slurp
                             (-> namespace
                                 str
                                 (string/replace #"-" "_")
                                 (string/split #"\.")
                                 (->> (string/join "/")
                                      (str "src/clj/"))
                                 (str ".clj"))
                             )}]})))

#_
(macroexpand-1
 '(ns-public-slurps [pod.epiccastle.bbssh.core
                     pod.epiccastle.bbssh.cleaner
                     ]))

(defn main []
  (let [server (ServerSocket. 0)
        port (.getLocalPort server)
        pid (.pid (java.lang.ProcessHandle/current))
        port-file (io/file (str ".babashka-pod-" pid ".port"))
        _ (.addShutdownHook (Runtime/getRuntime)
                            (Thread. (fn []
                                       (.delete port-file))))
        _ (spit port-file
                (str port "\n"))
        socket (.accept server)
        in (PushbackInputStream. (.getInputStream socket))
        out (.getOutputStream socket)
        ]
    (try
      (loop []
        (let [{:strs [id op var args ns]} (read in)
              id-decoded (safe-read id)
              op-decoded (safe-read op)
              var-decoded (safe-read var)
              args-decoded (safe-read args)
              ns-decoded (safe-read ns)]
          (debug 'id id-decoded
                 'op op-decoded
                 'var var-decoded
                 'args args-decoded
                 'ns ns-decoded)
          (case op-decoded
            "describe"
            (do
              (write out
                     {"port" port
                      "format" "edn"
                      "namespaces"
                      (concat
                       ;; pod side namespace
                       (ns-public-describes
                        [pod.epiccastle.bbssh.impl.core
                         pod.epiccastle.bbssh.impl.cleaner
                         pod.epiccastle.bbssh.impl.agent
                         pod.epiccastle.bbssh.impl.session
                         pod.epiccastle.bbssh.impl.channel-exec
                         pod.epiccastle.bbssh.impl.input-stream
                         pod.epiccastle.bbssh.impl.output-stream
                         ])

                       ;; bb side code
                       (ns-public-slurps
                        [pod.epiccastle.bbssh.core
                         pod.epiccastle.bbssh.cleaner
                         pod.epiccastle.bbssh.agent
                         pod.epiccastle.bbssh.session
                         pod.epiccastle.bbssh.channel-exec
                         pod.epiccastle.bbssh.input-stream
                         pod.epiccastle.bbssh.output-stream
                         ]))

                      "id" (read-string id)})
              (recur))
            "load-ns"
            (do
              ;;(prn "load-ns")
              (recur)
              )
            "invoke"
            (do
              (try
                (let [var (-> var
                              read-string
                              symbol)
                      args args-decoded]
                  (debug 'invoke var args)
                  (let [args (edn/read-string args)]
                    (if-let [f (lookup/lookup var)]
                      (let [value (pr-str (apply f args))
                            reply {"value" value
                                   "id" id
                                   "status" ["done"]}]
                        (debug 'reply reply)
                        (write out reply))
                      (throw (ex-info (str "Var not found: " var) {})))))
                (catch Throwable e
                  #_(binding [*out* *err*]
                      (println e))
                  (let [reply {"ex-message" (.getMessage e)
                               "ex-data" (pr-str
                                          (assoc (ex-data e)
                                                 :type (class e)))
                               "id" id
                               "status" ["done" "error"]}]
                    (write out reply))))
              (recur))
            (do
              (write out {"err" (str "unknown op:" (name op))})
              (recur)))))
      (catch java.io.EOFException _ nil))))
