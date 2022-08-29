(ns bbssh.impl.pod
  (:refer-clojure :exclude [read-string read])
  (:require [bbssh.impl.lookup :as lookup]
   [bencode.core :refer [read-bencode write-bencode]]
            [clojure.edn :as edn]
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
                      [

                       ;; pod invokes
                       {"name" "pod.epiccastle.bbssh.impl.core"
                        "vars" []}

                       {"name" "pod.epiccastle.bbssh.impl.cleaner"
                        "vars" [{"name" "del-reference"}
                                {"name" "get-references"}]}

                       {"name" "pod.epiccastle.bbssh.impl.agent"
                        "vars" [{"name" "new"}
                                {"name" "get-session"}]}

                       {"name" "pod.epiccastle.bbssh.impl.session"
                        "vars" [{"name" "new"}]}

                       ;; bb side code
                       {"name" "pod.epiccastle.bbssh.core"
                        "vars" [{"name" "_"
                                 "code" (slurp "src/clj/pod/epiccastle/bbssh/core.clj")}]}

                       {"name" "pod.epiccastle.bbssh.cleaner"
                        "vars" [{"name" "_"
                                 "code" (slurp "src/clj/pod/epiccastle/bbssh/cleaner.clj")}]}

                       {"name" "pod.epiccastle.bbssh.agent"
                        "vars" [{"name" "_"
                                 "code" (slurp "src/clj/pod/epiccastle/bbssh/agent.clj")}]}

                       {"name" "pod.epiccastle.bbssh.session"
                        "vars" [{"name" "_"
                                 "code" (slurp "src/clj/pod/epiccastle/bbssh/session.clj")}]}



                       ]
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
