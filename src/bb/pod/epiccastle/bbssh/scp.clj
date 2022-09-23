(ns pod.epiccastle.bbssh.scp
  "Implementation of the scp protocol"
  (:require [pod.epiccastle.bbssh.utils :as utils]
            [pod.epiccastle.bbssh.core :as bbssh]
            [clojure.string :as string]
            [clojure.java.io :as io])
  (:import [java.util Arrays]
           [java.io File]))

(defn- recv-ack [stream]
  (let [code (.read stream)]
    (when-not (zero? code)
      ;; read scp error message
      (let [msg (loop [c (.read stream)
                       s ""]
                  (if (#{10 13 -1 0} c)
                    s
                    (recur (.read stream) (str s (char c)))))]
        (throw (ex-info "scp error" {:type ::scp-error
                                     :code code
                                     :msg msg}))))))
(defn- send-ack
  "Send acknowledgement to the specified output stream"
  [stream]
  (.write stream (byte-array [0]))
  (.flush stream))

(defn- send-command
  "Send command to the specified output stream"
  [{:keys [in out]} cmd-string]
  (.write in (.getBytes (str cmd-string "\n")))
  (.flush in)
  (recv-ack out))

(defn io-copy-with-progress
  [source output-stream
   & [{:keys [size
              encoding
              progress-fn
              buffer-size
              progress-context]
       :or {buffer-size (* 256 1024)
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
    :or {buffer-size (* 256 1024)}}]
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
         buffer-size (* 256 1024)}
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
    (send-ack in)
    (recv-ack out)
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
         buffer-size (* 256 1024)}
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
      (send-ack in)
      (recv-ack out)
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
    (recv-ack out)
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
