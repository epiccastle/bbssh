(ns pod.epiccastle.bbssh.core
  (:require [pod.epiccastle.bbssh.agent :as agent]
            [pod.epiccastle.bbssh.session :as session]
            [pod.epiccastle.bbssh.utils :as utils]
            [clojure.string :as string]))

(def ^:private special-config-var-names
  {"kex" ["kex"]
   "server-host-key" ["server_host_key"]
   "prefer-known-host-key-types" ["prefer_known_host_key_types"]
   "enable-server-sig-algs" ["enable_server_sig_algs"]
   "cipher" ["cipher.s2c" "cipher.c2s"]
   "cipher-s2c" ["cipher.s2c"]
   "cipher-c2s" ["cipher.c2s"]
   "mac" ["mac.s2c" "mac.c2s"]
   "mac-s2c" ["mac.s2c"]
   "mac-c2s" ["mac.c2s"]
   "compression" ["compression.s2c" "compression.c2s"]
   "compression-s2c" ["compression.s2c"]
   "compression-c2s" ["compression.c2s"]
   "lang" ["lang.s2c" "lang.c2s"]
   "lang-s2c" ["lang.s2c"]
   "lang-c2s" ["lang.c2s"]
   "dhgex-min" ["dhgex_min"]
   "dhgex-max" ["dhgex_max"]
   "dhgex-preferred" ["dhgex_preferred"]
   "compression-level" ["compression_level"]
   "client-pubkey" ["PubkeyAcceptedAlgorithms"]})

(defn- process-session-connection-options
  "process the ssh connection-options setting and setup session
  accordingly"
  [session options]
  (doseq [[k v] options]
    (let [nk (name k)
          config-var-names
          (get special-config-var-names nk [nk])]
      (prn 'CFN config-var-names)
      (if (ifn? v)
        ;; function value based config
        (doseq [n config-var-names]
          (prn 'get-config session n '=> (session/get-config session n))
          (session/set-config
           session n
           (v (session/get-config session n))))
        ;; string value based config
        (doseq [n config-var-names]
          (session/set-config session n v))))))

(defn ssh
  "Start an SSH session. If connection is successful, returns the SSH
  session reference. If connection is unsuccessful, raises an
  exception. Requires a string for hostname.  Pass in an optional
  hashmap with the following keys:

  - `:port` The port to connect to. Defaults to 22.
  - `:username` The username to connect as. Defaults to the
    current username.
  - `:password` Pass in a string to use for password authentication.
  - `:identity` Pass in a file or filename of the private key to use
    in key authentication.
  - `:passphrase` Pass in a string to decrypt the private key with if
    needed.
  - `:private-key` Pass in a string (base64) or byte array of
    the private key to use for authentication.
  - `:public-key` Pass in a string (base64) or byte array of
    the public key associated with the private-key
  - `:agent-forwarding` If using an ssh-agent for authentication then
    turn on SSH agent authentication forwarding for this session.
    Default is `false`
  - `:strict-host-key-checking` Turn on or off strict host key checking.
    Default is `true`
  - `:known-hosts` A string defining the path to the known_hosts file
    to use. Is set to ~/.ssh/known_hosts by default.
  - `:accept-host-key` If `true` accept the host key if it is unknown.
    If `false` reject the host connection if the host key is unknown.
    If a string, accept the host-key only if the key fingerprint matches
    the string. Defaults to `false`.
  - `:connection-options` A hashmap of lesser used connection options.
    See below for details.
  - `:no-connect` Set to true to prevent the connection from being
    initiated. Just returns the prepared session reference. You will then
    need to call `session/connect` on it to initiate the connection.
  - `:agent` The bbssh agent to use to construct the session. If none
    is supplied a new bbssh agent will be created.
  - `:identity-repository` Use a custom identity-repository in the
    connection.
  - `:user-info` Use a custom user-info in the connection.
  - `:host-key-repository` Use a custom host-key-repository in the
    connection.

  The hashmap passed in `:connection-options` can have the following
  keys. Each key takes a string or a function as a configuration
  value.  If passed a string, that string becomes the set value. If
  passed a function this function should take a single string as an
  argument and return a string. It will be passed the existing
  configuration setting and should return the new configuration
  setting. These can be used to append or prepend extra values onto
  the default setting.

  `:kex`, `:server-host-key`, `:prefer-known-host-key-types`,
  `:enable-server-sig-algs`, `:cipher`, `:cipher-s2c`, `:cipher-c2s`,
  `:mac`, `:mac-s2c`, `:mac-c2s`, `:compression`, `:compression-s2c`,
  `:compression-c2s`, `:lang`, `:lang-s2c`, `:lang-c2s`, `:dhgex-min`,
  `:dhgex-max`, `:dhgex-preferred`, `:compression-level`,
  `:preferred-authentications`, `:client-pubkey`, `:check-ciphers`,
  `:check-macs`, `:check-kexes`, `:check-signatures`,
  `:fingerprint-hash`, `:max-auth-tries`
  "
  [hostname
   & [{:keys [agent port username password
              identity passphrase private-key public-key
              agent-forwarding strict-host-key-checking
              known-hosts accept-host-key connection-options
              no-connect identity-repository user-info
              host-key-repository]
       :or {port 22
            agent-forwarding false
            strict-host-key-checking true
            accept-host-key false
            connection-options {}}
       :as options}]]
  (let [username (or username (System/getProperty "user.name"))
        agent (or agent (agent/new))
        session (agent/get-session agent username hostname port)]
    (agent/set-known-hosts
     agent
     (or known-hosts
         (str (System/getProperty "user.home")
              "/.ssh/known_hosts")))
    (when password (session/set-password session password))
    (when identity
      (if passphrase
        (agent/add-identity agent identity passphrase)
        (agent/add-identity agent identity)))
    (when private-key
      (agent/add-identity
       agent
       (str "inline key for " username "@" hostname)
       (utils/opt-decode-base64 private-key)
       (utils/opt-decode-base64 (or public-key ""))
       (utils/opt-get-bytes (or passphrase ""))))
    (session/set-configs
     session
     (select-keys options [:agent-forwarding :strict-host-key-checking]))
    (when connection-options
      (process-session-connection-options session connection-options))
    (when identity-repository
      (session/set-identity-repository session identity-repository))
    (when user-info
      (session/set-user-info session user-info))
    (when host-key-repository
      (session/set-host-key-repository session host-key-repository))
    (when-not no-connect
      (session/connect session))
    session))
