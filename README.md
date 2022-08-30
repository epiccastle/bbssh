# bbssh
Babashka pod for SSH support.

This is a work in progress. It is not ready for use.

## Using

Here is a simple script that connects with a password, the runs a command and disconnects:

```clj
(ns test-simple.core
  (:require [babashka.pods :as pods]))

(pods/load-pod "./bbssh" {:transport :socket})

(require '[pod.epiccastle.bbssh.agent :as agent]
         '[pod.epiccastle.bbssh.session :as session])

(let [agent (agent/new)
      session (agent/get-session agent
                                 (System/getenv "USER")
                                 "localhost"
                                 22)]
  (session/set-config session :strict-host-key-checking false)
  (session/set-password session "my-password")
  (session/connect session)

  (let [channel (session/open-channel session "exec")
        input (input-stream/new)
        [out-stream out-in] (streams-for-out)
        [err-stream err-in] (streams-for-out)
        ]
    (input-stream/close input)
    (channel-exec/set-command channel "id")
    (channel-exec/set-input-stream channel input false)
    (channel-exec/set-output-stream channel out-stream)
    (channel-exec/set-error-stream channel err-stream)
    (channel-exec/connect channel)
    (let [buff (byte-array 1024)
          num (input-stream/read out-in buff 0 1024)]
      (println
       (String. (java.util.Arrays/copyOfRange buff 0 num) "UTF-8")))))

```

## Building

The bbssh pod is distributed as a static binary. Build it with:

```
$ make
```

This will generate the file `bbssh`.

## Running

### In Clojure

```
$ BABASHKA_POD=1 make run
clj -J-Djava.library.path=resources -m bbssh.core
...
```

### As Native Image

```
$ BABASHKA_POD=1 ./bbssh
```

## Namespace layout

There are conventions with the namespace layout you should know if you plan to extend this pod.

 - bbssh.impl.*

     These namespaces are internal functions related to implementing the pod itself

 - bbssh.*

     The basic programme mainline (used to setup and run the pod)

 - pod.epiccastle.bbssh.impl.*

     All public functions in these namespaces are exposed in the pod interface and will be called via pod `invoke`. These all run inside the pod native image. They have full access to the bbssh jvm heap.

 - pod.epiccastle.bbssh.*

     All code in these namespaces is injected into the babashka instance apon pod `describe`. These namespaces comprise the API you call when using bbssh from babashka.
