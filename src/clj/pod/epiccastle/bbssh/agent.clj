(ns pod.epiccastle.bbssh.agent
  (:require [pod.epiccastle.bbssh.impl.agent :as agent]
            [pod.epiccastle.bbssh.cleaner :as cleaner]))

(defn new []
  (cleaner/register (agent/new)))

(defn get-session [agent username host port]
  (cleaner/register
   (agent/get-session (cleaner/split-key agent) username host port)))
