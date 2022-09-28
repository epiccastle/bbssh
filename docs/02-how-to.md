# How To

```clojure
(require '[pod.epiccastle.bbssh.core :as bbssh])
```

## Debug the ssh connection process

Register an error reporting function with `pod.epiccastle.bbssh.agent/set-debug-fn` before initiating the connection

```clojure
(agent/set-debug-fn
 (fn [_level message]
   (binding [*out* *err*]
     (println message))))
```

Then when you connect you should see verbose error messages appear.

## Connect to a machine with a hard coded password

You can hard code a password in the options hash.

> **Note:** This is not recommended. You may accidentally commit your code to a repository with the password or inadvertantly expose it.

```clojure
(bbssh/ssh "remotehost"
    {:username "remoteusername"
     :port 22
     :password "the-password"})]
```

## Connect to a machine with an encrypted private key and a hard coded passphrase

You can hard code the passphrase in the options hash.

> **Note:** This is not recommended. You may accidentally commit your code to a repository with the passphrase or inadvertantly expose it.

```clojure
(bbssh/ssh "remotehost"
    {:username "remoteusername"
     :port 22
     :identity (str (System/getenv "HOME") "/.ssh/id_rsa")
     :passphrase "the-key-passphrase"})
```

## Turn off strict host key checking

```clojure
(bbssh/ssh "remotehost"
    {:strict-host-key-checking false})
```

## Accept a key and add it to known hosts without complaint only on first connection

```clojure
(bbssh/ssh "remotehost"
    {:accept-host-key :new})
```

## Accept a key if it matches a fingerprint

```clojure
(bbssh/ssh "remotehost"
    {:accept-host-key "SHA256:/tCQlmGVCXhwqJFq3h5aiEqD1UlUD9Eg5bDwd5yF52k"})
```
## Allow connection to a legacy server that only supports RSA/SHA1 signatures

```clojure
(bbssh/ssh "remotehost"
    {:connection-options
        {:server-host-key #(str % ",ssh-rsa")
         :client-pubkey #(str % ",ssh-rsa")}})
```

## Connect to a dropbear ssh server

A very old server:

```clojure
(bbssh/ssh "remotehost"
    {:connection-options
        {:kex "diffie-hellman-group1-sha1"})]
```

Or perhaps:

```clojure
(bbssh/ssh "remotehost"
    {:connection-options
        {:cipher "aes128-cbc"})]
```

## Execute a remote ssh command using authentication forwarding

```clojure
(-> (bbssh/ssh "remotehost")
    (bbssh/exec "ssh -o StrictHostKeyChecking=no git@github.com"
                {:err :string
                 :agent-forwarding true})
    deref
    :err
    clojure.string/split-lines
    second)
;; => "Hi retrogradeorbit! You've successfully authenticated, but GitHub does not provide shell access.\n"
```

## Allocate a pseudo terminal for the remote shell

```clojure
(-> (bbssh/ssh "remotehost")
    (bbssh/exec "tty"
                {:out :string
                 :pty true})
    deref
    :out)
;; => "/dev/pts/72\r\n"
```
