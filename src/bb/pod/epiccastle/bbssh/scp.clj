(ns pod.epiccastle.bbssh.scp
  "Implementation of the scp protocol"
  (:require [pod.epiccastle.bbssh.utils :as utils]
            [pod.epiccastle.bbssh.core :as bbssh]
            [clojure.string :as string]
            [clojure.java.io :as io])
  (:import [java.util Arrays]
           [java.io File]))

(def default-buffer-size (* 256 1024))

(defn- recv-ack [{:keys [out]}]
  (let [code (.read out)]
    (when-not (zero? code)
      ;; read scp error message
      (let [msg (loop [c (.read out)
                       s ""]
                  (if (#{10 13 -1 0} c)
                    s
                    (recur (.read out) (str s (char c)))))]
        (throw (ex-info "scp error" {:type ::scp-error
                                     :code code
                                     :msg msg}))))))
(defn- send-ack
  "Send acknowledgement to the specified output stream"
  [{:keys [in]}]
  (.write in (byte-array [0]))
  (.flush in))

(defn- send-command
  "Send command to the specified output stream"
  [{:keys [in] :as process} cmd-string]
  (.write in (.getBytes (str cmd-string "\n")))
  (.flush in)
  (recv-ack process))

(defn io-copy-with-progress
  [source output-stream
   & [{:keys [size
              encoding
              progress-fn
              buffer-size
              progress-context]
       :or {buffer-size default-buffer-size
            encoding "utf-8"}}]]
  (let [is-string? (string? source)
        is-file? (= File (class source))
        data (if is-string? (.getBytes source encoding) source)
        size (or size
                 (if is-file? (.length source) (count data)))
        input-stream (io/input-stream data)
        chunk (byte-array buffer-size)]
    (loop [offset 0
           progress-context progress-context]
      (if (= 0 size)
        (progress-fn progress-context source offset size)
        (let [bytes-read (.read input-stream chunk)]
          (if (= -1 bytes-read)
            progress-context
            (let [offset (+ offset bytes-read)]
              (io/copy
               (if (= bytes-read buffer-size)
                 chunk ;; full buffer read
                 (Arrays/copyOfRange chunk 0 bytes-read) ;; partial read
                 )
               output-stream)
              (.flush output-stream)
              (recur
               offset
               (progress-fn progress-context source offset size)))))))))

(defn io-copy-num-bytes
  [source output-stream
   length
   {:keys [buffer-size
           progress-context
           progress-fn]
    :or {buffer-size default-buffer-size}}]
  (let [buffer (byte-array buffer-size)]
    (loop [read-offset 0
           progress-context progress-context]
      (if (zero? length)
        (if progress-fn
          (progress-fn progress-context source read-offset length)
          progress-context)
        (let [bytes-read
              (.read source
                     buffer
                     0
                     (min (- length read-offset) buffer-size))]
          (if (= -1 bytes-read)
            progress-context
            (do
              (.write output-stream buffer 0 bytes-read)
              (let [read-offset (+ read-offset bytes-read)
                    progress-context
                    (if progress-fn
                      (progress-fn progress-context source read-offset length)
                      progress-context)]
                (if (< read-offset length)
                  (recur read-offset
                         progress-context)
                  progress-context)))))))))


(defn scp-copy-file
  [{:keys [in out] :as process}
   file
   {:keys [preserve mode buffer-size progress-fn]
    :or {mode 0644
         buffer-size default-buffer-size}
    :as options}]
  (send-command
   process
   (format "C%04o %d %s"
           (if preserve (utils/file-mode file) mode)
           (.length file)
           (.getName file)))
  (let [progress-context
        (if progress-fn
          (io-copy-with-progress file in options)
          (io/copy file in :buffer-size buffer-size))]
    (send-ack process)
    (recv-ack process)
    progress-context))

(defn scp-copy-dir
  [{:keys [in out] :as process}
   dir
   {:keys [preserve dir-mode progress-fn progress-context]
    :or {dir-mode 0755}
    :as options}]
  (send-command
   process
   (format "D%04o 0 %s"
           (if preserve (utils/file-mode dir) dir-mode)
           (.getName dir)))
  (let [progress-context
        (loop [[file & remain] (.listFiles dir)
               progress-context progress-context]
          (if file
            (cond
              (.isFile file)
              (recur
               remain
               (scp-copy-file process file
                              (assoc options
                                     :progress-context
                                     progress-context)))

              (.isDirectory file)
              (recur
               remain
               (scp-copy-dir process file
                             (assoc options
                                    :progress-context
                                    progress-context)))

              :else
              (recur remain progress-context))

            progress-context))]
    (send-command process "E")
    progress-context))

(defn scp-copy-data
  [{:keys [in out] :as process}
   [source info]
   {:keys [mode buffer-size progress-fn]
    :or {mode 0644
         buffer-size default-buffer-size}
    :as options}]
  (when-not (:name info)
    (throw (ex-info "scp data info must contain :name"
                    {:type ::name-error})))
  (let [data (if (string? source)
                 (.getBytes source (:encoding info "utf-8"))
                 source)
        size (or (:size info) (count data))]
    (send-command
     process
     (format "C%04o %d %s"
             (:mode info mode)
             size
             (:name info)))
    (let [progress-context
          (if progress-fn
            (io-copy-with-progress source in
                                   (assoc options
                                          :size size
                                          :encoding (:encoding info "utf-8")))
            (io/copy source in :buffer-size buffer-size))]
      (send-ack process)
      (recv-ack process)
      progress-context)))

(defn scp-to
  "copy local paths to remote path"
  [local-sources remote-path {:keys [session
                                     extra-flags
                                     recurse
                                     progress-context]
                              :or {extra-flags ""}
                              :as options}
   ]
  (let [remote-command
        (format
         "sh -c 'umask 0000; scp %s'"
         (string/join " "
                      [extra-flags
                       (when recurse "-r")
                       "-t" ;; to
                       (utils/quote-path remote-path)
                       ]))

        {:keys [in out err channel] :as process}
        (bbssh/exec session remote-command {:in :stream})]
    (recv-ack process)
    (loop [[source & remain] local-sources
           progress-context progress-context
           ]
      (let [options (assoc options :progress-context progress-context)
            progress-context
            (cond
              (vector? source)
              (scp-copy-data process source options)

              (.isDirectory ^File source)
              (scp-copy-dir process source options)

              (.isFile ^File source)
              (scp-copy-file process source options))]
        (if remain
          (recur remain progress-context)
          (do
            (.close in)
            (.close out)
            (.close err)
            progress-context))))))

(defn- scp-read-until-newline
  "Read from the remote process until a newline character.
  Assumes that the incoming data stops after the newline
  to wait for an ack."
  [{:keys [out]}]
  (let [buffer-size 4096
        buffer (byte-array buffer-size)]
    (loop [offset 0]
      (let [bytes-read (.read out buffer offset (- buffer-size offset))
            last-byte (aget buffer (dec (+ offset bytes-read)))]
        (if (= \newline (char last-byte))
          (String. buffer 0 (+ offset bytes-read))
          (recur (+ offset bytes-read)))))))

(defn scp-stream-to-file
  ""
  [{:keys [out in] :as process} file mode length
   {:keys [progress-fn
           buffer-size]
    :or {buffer-size default-buffer-size}
    :as options}]
  (with-open [file-stream (io/output-stream file)]
    (io-copy-num-bytes out file-stream length options)))

(defn scp-from-receive
  "Sink scp commands to file"
  [{:keys [out in] :as process}
   file {:keys [progress-fn
                progress-context]
         :as options}]
  ;;(prn file)
  (let [dir? (and (.exists file) (.isDirectory file))]
    (loop [command (scp-read-until-newline process)
           file file
           times nil
           depth 0
           context progress-context]
      (send-ack process)
      (prn "...." file ">" depth "[" times "]")
      (prn command)
      (case (first command)
        \C ;; create file
        (let [[mode length filename] (-> command
                                         string/trim
                                         (subs 1)
                                         (string/split #" " 3))
              mode (clojure.edn/read-string mode) ;; octal
              length (clojure.edn/read-string length)
              new-file (if dir? (File. file filename) file)]
          (when (.exists new-file)
            (.delete new-file))
          (utils/create-file file mode)
          (let [progress-context
                (scp-stream-to-file process file mode length options)]
            (recv-ack process)
            (send-ack process)
            progress-context
            ))
        )
      ;;(Thread/sleep 1000)
      #_(case (first command)
          \C (do
               (debug "\\C")
               (let [[mode length filename] (scp-parse-copy cmd)
                     nfile (if (and (.exists file) (.isDirectory file))
                             (File. file filename)
                             file)]
                 (when (.exists nfile)
                   (.delete nfile))
                 (nio/create-file nfile mode)
                 (let [new-context
                       (update (scp-sink-file send recv nfile mode length options context)
                               :fileset-file-start + length)]
                   (when times
                     (nio/set-last-modified-and-access-time nfile (first times) (second times)))
                   (if (pos? depth)
                     (recur (scp-receive-command send recv) file nil depth new-context)

                     ;; no more files. return
                     new-context
                     ))))
          \T (do
               (debug "\\T")
               (recur (scp-receive-command send recv) file (scp-parse-times cmd) depth context))
          \D (do
               (debug "\\D")
               (let [[mode _ ^String filename] (scp-parse-copy cmd)
                     dir (File. file filename)]
                 (when (and (.exists dir) (not (.isDirectory dir)))
                   (.delete dir))
                 (when (not (.exists dir))
                   (.mkdir dir))
                 (recur (scp-receive-command send recv) dir nil (inc depth) context)))
          \E (do
               (debug "\\E")
               (let [new-depth (dec depth)]
                 (when (pos? new-depth)
                   (recur (scp-receive-command send recv) (io/file (.getParent file)) nil new-depth context))))

          (when cmd
            (when (= 1 (int (first cmd)))
              ;; TODO: what to do with the error message?
              (let [[error next-cmd] (string/split (subs cmd 1) #"\n")]
                (println "WARNING:" error)
                (recur next-cmd file nil depth context))))))))

(defn scp-from
  "copy remote paths to local paths"
  [remote-path local-file {:keys [session
                                  extra-flags
                                  recurse]
                           :or {extra-flags ""}
                           :as options}
   ]
  (let [remote-command
        (format
         "scp %s"
         (string/join " "
                      [extra-flags
                       (when recurse "-r")
                       "-f" ;; from
                       (utils/quote-path remote-path)
                       ]))

        {:keys [in out err channel] :as process}
        (bbssh/exec session remote-command {:in :stream})]
    (send-ack process)
    (let [progress-context (scp-from-receive process local-file options)]
      (.close in)
      (.close out)
      (.close err)
      progress-context)))
