(ns pod.epiccastle.bbssh.channel-exec
  (:require [pod.epiccastle.bbssh.impl.channel-exec :as channel-exec]
            [pod.epiccastle.bbssh.cleaner :as cleaner]))

(defn set-command [channel command]
  (channel-exec/set-command
   (cleaner/split-key channel)
   command))

(defn connect [channel]
  (channel-exec/connect
   (cleaner/split-key channel)))

(defn set-input-stream
  ([channel input-stream dont-close?]
   (channel-exec/set-input-stream
    (cleaner/split-key channel)
    (cleaner/split-key input-stream)
    dont-close?))
  ([channel input-stream]
   (set-input-stream channel input-stream false)))

(defn set-output-stream
  ([channel output-stream dont-close?]
   (channel-exec/set-output-stream
    (cleaner/split-key channel)
    (cleaner/split-key output-stream)
    dont-close?))
  ([channel output-stream]
   (set-output-stream channel output-stream false)))

(defn set-error-stream
  ([channel error-stream dont-close?]
   (channel-exec/set-error-stream
    (cleaner/split-key channel)
    (cleaner/split-key error-stream)
    dont-close?))
  ([channel error-stream]
   (set-error-stream channel error-stream false)))

(defn get-input-stream
  [channel]
  (cleaner/register
   (channel-exec/get-input-stream
    (cleaner/split-key channel))))

(defn get-output-stream
  [channel]
  (cleaner/register
   (channel-exec/get-input-stream
    (cleaner/split-key channel))))

(defn get-error-stream
  [channel]
  (cleaner/register
   (channel-exec/get-input-stream
    (cleaner/split-key channel))))
