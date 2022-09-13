(ns pod.epiccastle.bbssh.agent
  (:require [pod.epiccastle.bbssh.impl.agent :as agent]
            [pod.epiccastle.bbssh.cleaner :as cleaner]))

(defn new
  "Make a new JSch agent. A JSch agent is not an \"ssh agent\".
  It is the base java class that holds and controls the
  sessions."
  []
  (cleaner/register (agent/new)))

(defn get-session
  "Construct a new JSch connection session. Does not start the ssh
  connection.
  "
  ([agent host]
   (cleaner/register
    (agent/get-session (cleaner/split-key agent) host)))
  ([agent username host]
   (cleaner/register
    (agent/get-session (cleaner/split-key agent) username host)))
  ([agent username host port]
   (cleaner/register
    (agent/get-session (cleaner/split-key agent) username host port))))

(defn get-identity-repository
  "Get the current identity-repository from the agent."
  [agent]
  (cleaner/register
   (agent/get-identity-repository
    (cleaner/split-key agent))))

(defn set-identity-repository
  "Set the identity-repository the agent should use."
  [agent identity-repository]
  (agent/set-identity-repository
   (cleaner/split-key agent)
   (cleaner/split-key identity-repository)))

(defn get-config-repository
  "Get the current config-repository from the agent."
  [agent]
  (cleaner/register
   (agent/get-config-repository
    (cleaner/split-key agent))))

(defn set-config-repository
  "Set the config-repository the agent should use."
  [agent config-repository]
  (agent/set-config-repository
   (cleaner/split-key agent)
   (cleaner/split-key config-repository)))

(defn get-host-key-repository
  "Get the current host-key-repository from the agent."
  [agent]
  (cleaner/register
   (agent/get-host-key-repository
    (cleaner/split-key agent))))

(defn set-host-key-repository
  "Set the host-key-repository the agent should use."
  [agent host-key-repository]
  (agent/set-host-key-repository
   (cleaner/split-key agent)
   (cleaner/split-key host-key-repository)))

(defn set-known-hosts
  "Set the known hosts file location"
  [agent filename]
  (agent/set-known-hosts
   (cleaner/split-key agent)
   filename))
