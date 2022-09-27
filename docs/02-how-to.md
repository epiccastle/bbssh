# How To

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
