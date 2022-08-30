(ns pod.epiccastle.bbssh.agent
  (:require [pod.epiccastle.bbssh.impl.agent :as agent]
            [pod.epiccastle.bbssh.cleaner :as cleaner]))

(defn new
  "Make a new JSch agent. A JSch agent is not an \"ssh agent\".
  It is simply the base java class that holds and controls the
  sessions."
  []
  (cleaner/register (agent/new)))

(defn get-session
  "From the JSch agent, construct a new JSch session. Does not
  start the ssh connection.
  "
  [agent username host port]
  (cleaner/register
   (agent/get-session (cleaner/split-key agent) username host port)))
