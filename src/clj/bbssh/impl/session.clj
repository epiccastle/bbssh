(ns bbssh.impl.session
  (:require [bbssh.impl.utils :as utils])
  (:import [com.jcraft.jsch JSch Session]))

(set! *warn-on-reflection* true)

(defn- setconfig-fn [& conf-keys]
  (fn [^Session obj val]
    (doseq [conf-key conf-keys]
      (.setConfig obj conf-key val))))

(defn ^Session make-session
  "Start a SSH session. Requires JSch agent and hostname.
  You also pass an options hashmap with values for :username,
  :password, :port, :identity, :passphrase, :private-key,
  :public-key, :agent-forwarding, :strict-host-key-checking,
  :accept-host-key and :jsch-options keys."
  [^JSch agent hostname
   {:keys [port username password
           identity passphrase
           private-key public-key
           jsch-options]
    :or {port 22
         jsch-options {}}
    :as options}]
  (let [session-options
        (select-keys options [:agent-forwarding
                              :strict-host-key-checking
                              :accept-host-key])

        session ^Session (.getSession agent username hostname port)]
    (when password (.setPassword session ^String password))

    (when identity
      (if passphrase
        (.addIdentity agent ^String identity ^String passphrase)
        (.addIdentity agent ^String identity)))

    (when private-key
      (.addIdentity agent
                    (if (:username options)
                      (format "inline key for %s@%s" username hostname)
                      (format "inline key for %s" hostname)
                      )
                    (utils/string-to-byte-array private-key)
                    (utils/string-to-byte-array (or public-key ""))
                    (utils/string-to-byte-array (or passphrase "")))
      )
    ;; :strict-host-key-checking
    (doseq [[k v] session-options]
      (when-not (nil? v)
        (.setConfig session
                    (utils/to-camel-case k)
                    (case v
                      true "yes"
                      false "no"
                      (name v)))))

    ;; jsch-options
    (doseq [[k v] jsch-options]
      (let [fs
            {:kex (setconfig-fn "kex")
             :server-host-key (setconfig-fn "server_host_key")
             :prefer-known-host-key-types (setconfig-fn "server_host_key")
             :enable-server-sig-algs (setconfig-fn "enable_server_sig_algs")
             :cipher (setconfig-fn "cipher.s2c"
                                   "cipher.c2s")
             :cipher-s2c (setconfig-fn "cipher.s2c")
             :cipher-c2s (setconfig-fn "cipher.c2s")
             :mac (setconfig-fn "mac.s2c"
                                "mac.c2s")
             :mac-s2c (setconfig-fn "mac.s2c")
             :mac-c2s (setconfig-fn "mac.c2s")
             :compression (setconfig-fn "compression.s2c"
                                        "compression.c2s")
             :compression-s2c (setconfig-fn "compression.s2c")
             :compression-c2s (setconfig-fn "compression.c2s")
             :lang (setconfig-fn "lang.s2c"
                                 "lang.c2s")
             :lang-s2c (setconfig-fn "lang.s2c")
             :lang-c2s (setconfig-fn "lang.c2s")
             :dhgex-min (setconfig-fn "dhgex_min")
             :dhgex-max (setconfig-fn "dhgex_max")
             :dhgex-preferred (setconfig-fn "dhgex_preferred")
             :compression-level (setconfig-fn "compression_level")
             :preferred-authentications (setconfig-fn "PreferredAuthentications")
             :client-pubkey (setconfig-fn "PubkeyAcceptedAlgorithms")
             :check-ciphers (setconfig-fn "CheckCiphers")
             :check-macs (setconfig-fn "CheckMacs")
             :check-kexes (setconfig-fn "CheckKexes")
             :check-signatures (setconfig-fn "CheckSignatures")
             :fingerprint-hash (setconfig-fn "FingerprintHash")
             :max-auth-tries (setconfig-fn "MaxAuthTries")}
            f (fs k)]
        (when (and f (not (nil? v)))
          (f session v))))
    session))
