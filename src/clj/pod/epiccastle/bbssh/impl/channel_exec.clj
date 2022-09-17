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

(defn set-pty [channel enable]
  (.setPty
   ^ChannelExec (references/get-instance channel)
   ^boolean enable))

(defn set-pty-size [channel col row width-pixels height-pixels]
  (.setPtySize
   ^ChannelExec (references/get-instance channel)
   ^int col
   ^int row
   ^int width-pixels
   ^int height-pixels))

(defn set-pty-type
  ([channel terminal-type]
   (.setPtyType
    ^ChannelExec (references/get-instance channel)
    ^String terminal-type))
  ([channel terminal-type col row width-pixels height-pixels]
   (.setPtyType
    ^ChannelExec (references/get-instance channel)
    ^String terminal-type
    ^int col
    ^int row
    ^int width-pixels
    ^int height-pixels)))

(defn set-terminal-mode
  [channel terminal-mode]
  (.setTerminalMode
   ^ChannelExec (references/get-instance channel)
   ^bytes (utils/decode-base64 terminal-mode)))

(defn set-agent-forwarding [channel enable]
  (.setAgentForwarding
   ^ChannelExec (references/get-instance channel)
   ^boolean enable))

(defn set-x-forwarding [channel enable]
  (.setAgentForwarding
   ^ChannelExec (references/get-instance channel)
   ^boolean enable))

(defn set-env [channel name value]
  (.setEnv
   ^ChannelExec (references/get-instance channel)
   ^String name
   ^String value))
