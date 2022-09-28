# Basic Use

## Loading

```clojure
(ns my-program
  (:require [babashka.pods :as pods]))

(pods/load-pod "./bbssh" {:transport :socket})
```

## Requiring

```clojure
(require '[pod.epiccastle.bbssh.core :as bbssh])
```

## Connecting

Use `pod.epiccastle.bbssh.core/ssh` to open an ssh connection to a remote host. The returned session value will be a reference keyword of the following form:

```clojure
(bbssh/ssh "localhost")
;; => :com.jcraft.jsch/Session-0r0e6yi4m9j8yabp
```

Here we open an ssh connection as a user on a remote host:

```clojure
(let [session (bbssh/ssh "remotehost"
                         {:username "remoteusername"
                          :port 22})]
  ;; use session
  )
```

If an ssh-agent and valid key is available it will be used for authentication. If the authentication method falls back to password and one is not available the user will be prompted at the terminal for the password.

You can specify a private key file to use for authentication with the option `:identity`:

```clojure
(let [session (bbssh/ssh "remotehost"
                    {:username "remoteusername"
                     :port 22
                     :identity "path/to/id_rsa"})]
  ;; use session
  )
```

Alternatively you can specify the contents of a private key to use for authentication with `:private-key`

```
(let [session (bbssh/ssh "remotehost"
                    {:username "remoteusername"
                     :port 22
                     :private-key (slurp "path/to/id_rsa")})]
  ;; use session
  )
```

## Execution

To execute a remote process on the session use `pod.epiccastle.bbssh.core/exec`. This returns an `pod.epiccastle.bbssh.SshProcess` record:

```clojure
(bbssh/exec session "echo 'I am running over ssh'")
;; => #pod.epiccastle.bbssh.SshProcess{:channel :com.jcraft.jsch/ChannelExec-i9qp6i1wk1uiqpio, :exit nil, :in nil, :out #object[babashka.impl.proxy.proxy$java.io.PipedInputStream$ff19274a 0x6f9c2c47 "babashka.impl.proxy.proxy$java.io.PipedInputStream$ff19274a@6f9c2c47"], :err #object[babashka.impl.proxy.proxy$java.io.PipedInputStream$ff19274a 0x207ed897 "babashka.impl.proxy.proxy$java.io.PipedInputStream$ff19274a@207ed897"], :prev nil, :cmd "echo 'I am running over ssh'"}
```

By default no `:in` stream will be used. Streams will be returned for `:out` and `:err`. The funtion will return immediately and `:exit` will be nil as an indicator the process is still running. Dereferencing the return value will block until the process is finished and fill in the exit value of the remote process:

```clojure
(:exit @(bbssh/exec session "exit 3"))
;; => 3
```

Specify the format for the final value of `:out` and `:err` by passing in options:

```clojure
(-> @(bbssh/exec session "echo stdout; echo stderr 1>&2; exit 3"
                 {:out :string
                  :err :string})
    (select-keys [:exit :err :out]))
;; => {:exit 3, :err "stderr\n", :out "stdout\n"}
```

```clojure
(-> @(bbssh/exec session "echo stdout"
                 {:out :bytes})
       :out
       seq)
;; => (115 116 100 111 117 116 10)
```

## Streaming between local and remote

Bbssh interoperates with [babashka.process](https://github.com/babashka/process) pipelining. The first argument of `pod.epiccastle.bbssh.core/exec` can be substituted for an `SshProcess` or a babashka.process `Process`. When doing so you need to pass in the session via theoptions hashmap. Here we stream between from a local process, to a remote process and back to a local process:

```clojure
(-> (babashka.process/process "echo this is local")
    (bbssh/exec "md5sum" {:session session})
    (babashka.process/process
        "bash -c \"echo 'our sum: $(cat)'\""
        {:out :string})
       deref
       :out)
;; => "our sum: 4088b54321c3a731eda432ab09fa9f63 -\n"
```

## Streaming between remotes

In a similar way we can stream from one remote execution to another.

```clojure
(-> (bbssh/exec session-one "echo first host")
    (bbssh/exec "echo $(cat) and now second host"
                {:session session-two
                 :out :string})
    deref
    :out)
;; => "first host and now second host\n"
```

> **Note:** This streamed data is transfered via your local machine. So it doesn't make sense to stream from one process to another on the same remote host. In those cases use a pipe in the command to stream the data. This technique is useful for streaming from a remote process on one machine to a remote process on another machine.

## Copying files from local to remote

Scp functionality is in the `pod.epiccastle.bbssh.scp` namespace:

```clojure
(require '[pod.epiccastle.bbssh.scp :as scp])
```

You can scp files from the local filesystem to a remote machine with `pod.epiccastle.bbssh.scp/scp-to`.

```clojure
(scp/scp-to [(io/as-file "file1.dat") (io/as-file "file2.dat")]
            "remote-path"
            {:session session})
;; => nil
```

> **Note:** Remote paths are relative to the home directory of the remote user.

## Copying directories from local to remote

You can recursively copy from a local directory to a remote machine by setting the option `:recurse?` to `true`:

```clojure
(scp/scp-to [(io/as-file "a-directory")]
            "remote-path"
            {:session session
             :recurse? true})
;; => nil
```

## Copying a remote file to the local machine

You can scp files from the local filesystem to a remote machine with `pod.epiccastle.bbssh.scp/scp-from`:

```clojure
(scp/scp-from "remote-file.dat"
              "local-destination"
              {:session session})
;; => nil
```

## Copying a remote directory to the local machine

You can recursively copy from a remote directory to the local machine by setting the option `:recurse?` to `true`:

```clojure
(scp/scp-from "remote-directory"
              "local-destination"
              {:session session
               :recurse? true})
;; => nil
```
