(ns pod.epiccastle.bbssh.impl.channel-exec
  (:require [bbssh.impl.references :as references]
            [bbssh.impl.utils :as utils])
  (:import [com.jcraft.jsch JSch Session UserInfo ChannelExec Channel ChannelSession]
           [java.io InputStream OutputStream])
  )

;; pod.epiccastle.bbssh.impl.* are invoked on pod side.

(set! *warn-on-reflection* true)

(defn set-command [channel command]
  (.setCommand
   ^ChannelExec (references/get-instance channel)
   ^String command))

(defn connect [channel]
  (.connect
   ^ChannelExec (references/get-instance channel)))

(defn set-input-stream [channel input-stream dont-close?]
  (.setInputStream
   ^ChannelExec (references/get-instance channel)
   ^InputStream (references/get-instance input-stream)
   ^Boolean dont-close?))

(defn set-output-stream [channel output-stream dont-close?]
  (.setOutputStream
   ^ChannelExec (references/get-instance channel)
   ^OutputStream (references/get-instance output-stream)
   ^Boolean dont-close?))

(defn set-error-stream [channel error-stream dont-close?]
  (.setErrStream
   ^ChannelExec (references/get-instance channel)
   ^OutputStream (references/get-instance error-stream)
   ^Boolean dont-close?))

(defn get-input-stream [channel]
  (references/add-instance
   (.getInputStream
    ^ChannelExec (references/get-instance channel))))

(defn get-error-stream [channel]
  (references/add-instance
   (.getErrStream
    ^ChannelExec (references/get-instance channel))))

(defn get-output-stream [channel]
  (references/add-instance
   (.getOutputStream
    ^ChannelExec (references/get-instance channel))))
