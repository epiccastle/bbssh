(ns pod.epiccastle.bbssh.channel-exec
  (:require [pod.epiccastle.bbssh.impl.channel-exec :as channel-exec]
            [pod.epiccastle.bbssh.cleaner :as cleaner]
            [pod.epiccastle.bbssh.utils :as utils]))

(defn set-command [channel command]
  (channel-exec/set-command
   (cleaner/split-key channel)
   command))

(defn connect [channel]
  (channel-exec/connect
   (cleaner/split-key channel)))

(defn disconnect [channel]
  (channel-exec/disconnect
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

(defn set-pty [channel enable]
  (channel-exec/set-pty
   (cleaner/split-key channel)
   enable))

(defn set-pty-size [channel col row width-pixels height-pixels]
  (channel-exec/set-pty
   (cleaner/split-key channel)
   col
   row
   width-pixels
   height-pixels))

(defn set-pty-type
  ([channel terminal-type]
   (channel-exec/set-pty-type
    (cleaner/split-key channel)
    terminal-type)
   )
  ([channel terminal-type col row width-pixels height-pixels]
   (channel-exec/set-pty-type
    (cleaner/split-key channel)
    terminal-type
    col
    row
    width-pixels
    height-pixels)))

(defn set-terminal-mode
  [channel terminal-mode]
  (channel-exec/set-terminal-mode
   (cleaner/split-key channel)
   (utils/encode-base64 terminal-mode)))

(defn set-agent-forwarding [channel enable]
  (channel-exec/set-agent-forwarding
   (cleaner/split-key channel)
   enable))

(defn set-x-forwarding [channel enable]
  (channel-exec/set-agent-forwarding
   (cleaner/split-key channel)
   enable))

(defn set-env [channel name value]
  (channel-exec/set-env
   (cleaner/split-key channel)
   name
   value))

(defn is-closed [channel]
  (channel-exec/is-closed
   (cleaner/split-key channel)))

(defn is-connected [channel]
  (channel-exec/is-connected
   (cleaner/split-key channel)))

(defn send-signal [channel signal]
  (channel-exec/send-signal
   (cleaner/split-key channel)
   signal))

(defn get-exit-status [channel]
  (channel-exec/get-exit-status
   (cleaner/split-key channel)))

(defn get-id [channel]
  (channel-exec/get-id
   (cleaner/split-key channel)))
