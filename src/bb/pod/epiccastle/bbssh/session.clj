(ns pod.epiccastle.bbssh.session
  (:require [pod.epiccastle.bbssh.impl.session :as session]
            [pod.epiccastle.bbssh.cleaner :as cleaner]))

(defn set-password
  "Set the password the session will use to authenticate to
  `password`"
  [session password]
  (session/set-password
   (cleaner/split-key session)
   password))

(defn set-user-info
  "Set the user-info structure the session will use to gather
  keys and associated information from to `user-info`"
  [session user-info]
  (session/set-user-info
   (cleaner/split-key session)
   (cleaner/split-key user-info)))

(defn connect
  "Initiate the ssh connection with an optional `timeout`
  (in milliseconds)."
  [session & [timeout]]
  (session/connect
   (cleaner/split-key session)
   timeout))

(defn disconnect
  "Disconnect the ssh connection"
  [session]
  (session/disconnect
   (cleaner/split-key session)))

(defn set-port-forwarding-local
  "Register the local port to forward all connection to the remote
  side, where they will connect to a remote host on a port.

  `options` is a hashmap for the following form

  ```clj
  {
    :bind-address \"127.0.0.1\"              ;; the local interface to bind to. Use \"*\" or \"0.0.0.0\" for all interfaces.
    :local-port 2200                         ;; the local port to listen on
    :remote-host \"jump-target.domain.com\"  ;; the remote host to forward the connection to on the remote side
    :remote-port 22                          ;; the remote port to forward to
    :connect-timeout 30000                   ;; how long to try to connect for
  }
  ```
  "
  [session options]
  (session/set-port-forwarding-local
   (cleaner/split-key session)
   options))

(defn delete-port-forwarding-local
  "Cancels the specified local port forwarding"
  [session options]
  (session/delete-port-forwarding-local
   (cleaner/split-key session)
   options))

(defn get-port-forwarding-local
  "return a list of all the local port forwards. List elements
  are of the form \"local-port:host:host-port\"."
  [session]
  (session/get-port-forwarding-local
   (cleaner/split-key session)))

(defn set-host
  "Set the host to connect to"
  [session host]
  (session/set-host
   (cleaner/split-key session)
   host))

(defn set-port
  "Set the port to connect to"
  [session port]
  (session/set-port
   (cleaner/split-key session)
   port))

(defn set-config
  "Set the config setting `key` to `value`"
  [session key value]
  (session/set-config
   (cleaner/split-key session)
   key
   value))

(defn get-config
  "Get the current config setting `key`"
  [session key]
  (session/get-config
   (cleaner/split-key session)
   key))

(defn connected?
  "return true if session is currently connected"
  [session]
  (session/connected?
   (cleaner/split-key session)))

(defn open-channel
  "open a channel on the session and return it"
  [session type]
  (cleaner/register
   (session/open-channel
    (cleaner/split-key session)
    type)))