(ns bbssh.impl.pod
  (:refer-clojure :exclude [read-string read])
  (:require [bbssh.impl.lookup :as lookup]
            [pod.epiccastle.bbssh.pod.core]
            [pod.epiccastle.bbssh.pod.cleaner]
            [pod.epiccastle.bbssh.pod.agent]
            [pod.epiccastle.bbssh.pod.session]
            [pod.epiccastle.bbssh.pod.channel-exec]
            [pod.epiccastle.bbssh.pod.input-stream]
            [pod.epiccastle.bbssh.pod.output-stream]
            [pod.epiccastle.bbssh.pod.byte-array-input-stream]
            [pod.epiccastle.bbssh.pod.byte-array-output-stream]
            [pod.epiccastle.bbssh.pod.user-info]
            [pod.epiccastle.bbssh.pod.identity]
            [pod.epiccastle.bbssh.pod.identity-repository]
            [pod.epiccastle.bbssh.pod.callbacks]
            [pod.epiccastle.bbssh.pod.key-pair]
            [pod.epiccastle.bbssh.pod.host-key]
            [pod.epiccastle.bbssh.pod.known-hosts]
            [pod.epiccastle.bbssh.pod.host-key-repository]
            [pod.epiccastle.bbssh.pod.terminal]
            [pod.epiccastle.bbssh.pod.socket]
            [pod.epiccastle.bbssh.pod.config]
            [pod.epiccastle.bbssh.pod.config-repository]
            [bencode.core :refer [read-bencode write-bencode]]
            [clojure.edn :as edn]
            [clojure.string :as string]
            [clojure.java.io :as io])
  (:import [java.io PushbackInputStream]
           [java.net ServerSocket]))

(set! *warn-on-reflection* true)

(def debug? false)
(def debug-file "/tmp/bbssh-pod-debug.txt")

(defn read-string [^"[B" v]
  (String. v))

(defmacro debug [& args]
  (if debug?
    `(with-open [wrtr# (io/writer debug-file :append true)]
       (.write wrtr# (prn-str ~@args)))
    nil))

(def stdin (PushbackInputStream. System/in))

(def write-lock (Object.))

(defn write [out v]
  (locking write-lock
    (write-bencode out v)))

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
                                      (str "src/bb/"))
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
        _ (spit port-file
                (str port "\n"))
        socket (.accept server)
        _ (.setTcpNoDelay socket true)
        in (PushbackInputStream. (.getInputStream socket))
        out (.getOutputStream socket)
        ]
    (try
      (loop []
        (let [{:strs [id op var args ns]} (read in) ;; blocking read
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
                      "ops" {"shutdown" {}}
                      "namespaces"
                      (concat
                       ;; this comes first to create an internal name
                       ;; for the pod to use in manual invoke calls
                       [{"name" "pod.epiccastle.bbssh"
                         "vars" []}]
                       ;; pod side namespace
                       (ns-public-describes
                        [pod.epiccastle.bbssh.pod.terminal
                         pod.epiccastle.bbssh.pod.cleaner
                         pod.epiccastle.bbssh.pod.agent
                         pod.epiccastle.bbssh.pod.session
                         pod.epiccastle.bbssh.pod.channel-exec
                         pod.epiccastle.bbssh.pod.input-stream
                         pod.epiccastle.bbssh.pod.output-stream
                         pod.epiccastle.bbssh.pod.byte-array-input-stream
                         pod.epiccastle.bbssh.pod.byte-array-output-stream
                         pod.epiccastle.bbssh.pod.user-info
                         pod.epiccastle.bbssh.pod.identity
                         pod.epiccastle.bbssh.pod.identity-repository
                         pod.epiccastle.bbssh.pod.callbacks
                         pod.epiccastle.bbssh.pod.key-pair
                         pod.epiccastle.bbssh.pod.host-key
                         pod.epiccastle.bbssh.pod.known-hosts
                         pod.epiccastle.bbssh.pod.host-key-repository
                         pod.epiccastle.bbssh.pod.config
                         pod.epiccastle.bbssh.pod.config-repository
                         pod.epiccastle.bbssh.pod.core
                         pod.epiccastle.bbssh.pod.socket
                         ])

                       ;; bb side code
                       (ns-public-slurps
                        [pod.epiccastle.bbssh.terminal
                         pod.epiccastle.bbssh.impl.cleaner
                         pod.epiccastle.bbssh.impl.utils
                         pod.epiccastle.bbssh.impl.socket
                         pod.epiccastle.bbssh.impl.pack
                         pod.epiccastle.bbssh.agent
                         pod.epiccastle.bbssh.session
                         pod.epiccastle.bbssh.channel-exec
                         pod.epiccastle.bbssh.input-stream
                         pod.epiccastle.bbssh.output-stream
                         pod.epiccastle.bbssh.byte-array-input-stream
                         pod.epiccastle.bbssh.byte-array-output-stream
                         pod.epiccastle.bbssh.user-info
                         pod.epiccastle.bbssh.identity
                         pod.epiccastle.bbssh.identity-repository
                         pod.epiccastle.bbssh.key-pair
                         pod.epiccastle.bbssh.host-key
                         pod.epiccastle.bbssh.known-hosts
                         pod.epiccastle.bbssh.host-key-repository
                         pod.epiccastle.bbssh.config
                         pod.epiccastle.bbssh.config-repository
                         pod.epiccastle.bbssh.ssh-agent
                         pod.epiccastle.bbssh.core
                         pod.epiccastle.bbssh.scp]))

                      "id" (read-string id)})
              (recur))
            "load-ns"
            (recur)

            "shutdown"
            (.delete port-file)

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
                      (let [f-meta (meta f)]
                        (cond
                          (:async f-meta)
                          ;; functions marked async are passed their reply function
                          ;; as a first argument
                          (apply f (fn [value & [status]]
                                     (write out {"value" (pr-str value)
                                                 "id" id
                                                 "status" (or status [])}))
                                 args)


                          (:blocking f-meta)
                          ;; functions marked blocking are invoked in a thread so we can
                          ;; still process messages that come to the pod. These messages
                          ;; need to be processed in order to complete *this* blocking
                          ;; function.
                          (future
                            (try
                              (let [value (pr-str (apply f args))
                                    _ (debug 'value value)
                                    reply {"value" value
                                           "id" id
                                           "status" ["done"]}]
                                (write out reply))
                              (catch Throwable e
                                (let [reply {"ex-message" (.getMessage e)
                                             "ex-data" (pr-str
                                                        (assoc (ex-data e)
                                                               :type (class e)))
                                             "id" id
                                             "status" ["done" "error"]}]
                                  (write out reply)))))

                          :else
                          ;; normal synchronous invoke
                          (do
                            ;;(prn 'invoke-blocking var)
                            (let [value (pr-str (apply f args))
                                  _ (debug 'value value)
                                  reply {"value" value
                                         "id" id
                                         "status" ["done"]}]
                              (write out reply)))))
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
      (.close socket)
      (catch java.io.EOFException _ nil))))
