(ns pod.epiccastle.bbssh.agent
  (:require [pod.epiccastle.bbssh.impl.agent :as agent]
            [pod.epiccastle.bbssh.utils :as utils]
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

(defn set-known-hosts-content
  "Set the known hosts file location"
  [agent content]
  (agent/set-known-hosts-content
   (cleaner/split-key agent)
   (utils/encode-base64 content)))

(defn add-identity
  "Add the private key to be used in authentication. Optionally
  add the public key aswell. Private key can be decrypted with passphrase."
  ([agent private-key-filename]
   (agent/add-identity
    (cleaner/split-key agent)
    private-key-filename))
  ([agent private-key-filename-or-identity passphrase]
   (if (string? private-key-filename-or-identity)
     ;; filename string is 2nd arg
     (if (string? passphrase)
       (agent/add-identity
        (cleaner/split-key agent)
        private-key-filename-or-identity
        passphrase)
       (agent/add-identity2
        (cleaner/split-key agent)
        private-key-filename-or-identity
        (utils/encode-base64 passphrase)))
     ;; identity reference is 2nd arg
     (agent/add-identity3
      (cleaner/split-key agent)
      (cleaner/split-key private-key-filename-or-identity)
      (utils/encode-base64 passphrase))
     ))
  ([agent private-key-filename public-key-filename passphrase]
   (agent/add-identity
    (cleaner/split-key agent)
    private-key-filename
    public-key-filename
    (utils/encode-base64 passphrase)))
  ([agent ^String identity-name ^bytes private-key ^bytes public-key ^bytes passphrase]
   (agent/add-identity
    (cleaner/split-key agent)
    identity-name
    (utils/encode-base64 private-key)
    (utils/encode-base64 public-key)
    (utils/encode-base64 passphrase))))

(defn remove-identity
  "remove an identity by its name or its reference"
  [agent identity]
  (if (string? identity)
    (agent/remove-identity
     (cleaner/split-key agent)
     identity)
    (agent/remove-identity2
     (cleaner/split-key agent)
     (cleaner/split-key identity))))

(defn get-identity-names
  "Lists names of identities included in the identity-repository"
  [agent]
  (agent/get-identity-names
   (cleaner/split-key agent)))

(defn remove-all-identities
  "Removes all identities from the identity-repository."
  [agent]
  (agent/remove-all-identities
   (cleaner/split-key agent)))

(defn get-config
  "Returns the config value for the specified key"
  [key]
  (agent/get-config key))

(defn set-config
  "Sets or overrides the configuration."
  ([hashmap]
   (agent/set-config hashmap))
  ([key value]
   (agent/set-config key value)))
