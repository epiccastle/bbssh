# How To

```clojure
(require '[pod.epiccastle.bbssh.core :as bbssh])
```

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
     :passphrase "the-key-passphrase"})]
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
