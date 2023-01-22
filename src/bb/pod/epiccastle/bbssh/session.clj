(ns pod.epiccastle.bbssh.session
  "Creates and calls the various methods of a Session that
  exists on the pod heap."
  (:require [pod.epiccastle.bbssh.pod.session :as session]
            [pod.epiccastle.bbssh.impl.cleaner :as cleaner]))

(defn set-password
  "Set the password the session will use to authenticate to
  `password`"
  [session password]
  (session/set-password
   (cleaner/split-key session)
   password))

(defn set-user-info
  "Set the `user-info` for the `session`. The session will
  use this user-info structure to ask for passwords and passphrases."
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
    :local-port 2200                       ;; the local port to listen on
    :remote-host \"jump-target.domain.com\"  ;; the remote host to forward the connection to on the remote side
    :remote-port 22                        ;; the remote port to forward to
    :connect-timeout 30000                 ;; how long to try to connect for
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

(defn set-port-forwarding-remote
  "Register the remote port to forward to the local machine and then
  connect out to a host on the local network.

  `options` is a hashmap for the following form

  ```clj
  {
    :bind-address \"127.0.0.1\"            ;; the remote interface to bind to. Use \"*\" or \"0.0.0.0\" for all interfaces.
    :remote-port 22                        ;; the remote port to bind to
    :local-host \"host.localdomain\"       ;; the local network host to forward the connection to on the local side
    :local-port 2200                       ;; the local port to connect to
    :connect-timeout 30000                 ;; how long to try to connect for
  }
  ```
  "
  [session options]
  (session/set-port-forwarding-remote
   (cleaner/split-key session)
   options))

(defn delete-port-forwarding-remote
  "Cancels the specified remote port forwarding"
  [session options]
  (session/delete-port-forwarding-remote
   (cleaner/split-key session)
   options))

(defn get-port-forwarding-remote
  "return a list of all the remote port forwards. List elements
  are of the form \"local-port:host:host-port\"."
  [session]
  (session/get-port-forwarding-remote
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

(defn set-configs
  "Merge the config values from the passed in hashmap into the session
  config"
  [session hashmap]
  (session/set-configs
   (cleaner/split-key session)
   hashmap))

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

(defn set-identity-repository
  "sets the identity-repository that will be used in the
  public key authentication"
  [session identity-repository]
  (session/set-identity-repository
   (cleaner/split-key session)
   (cleaner/split-key identity-repository)))

(defn set-host-key-repository
  "sets the host-key-repository that will be used in the
  public key authentication"
  [session host-key-repository]
  (session/set-host-key-repository
   (cleaner/split-key session)
   (cleaner/split-key host-key-repository)))

(defn set-proxy
  "sets the http/socks proxy to connect with the ssh server.

  The provided arg must have at least `:type` (one of
  `#{:http :socks4 :socks5}`), `:host`, `:port` and optionally `:username` and
  `:password` for proxy authentication. "

  [session {:keys [type] :as proxy}]
  (assert (#{:http :socks4 :socks5} type)
    (format "Invalid proxy type '%s'" type))
  (session/set-proxy
   (cleaner/split-key session)
   proxy))
