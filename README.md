# bbssh
Babashka pod for SSH support.

[![CircleCI](https://circleci.com/gh/epiccastle/bbssh/tree/master.svg?style=shield)](https://circleci.com/gh/epiccastle/bbssh/tree/master)
[![Babashka](https://raw.githubusercontent.com/babashka/babashka/master/logo/badge.svg)](https://github.com/babashka/babashka)

This is a work in progress. It is not ready for use.

## Using

Here is a simple script that connects with a password, the runs a command and disconnects:

```clj
(ns test-simple.core
  (:require [babashka.pods :as pods]))

(pods/load-pod "./bbssh" {:transport :socket})

(require '[pod.epiccastle.bbssh.agent :as agent]
         '[pod.epiccastle.bbssh.session :as session]
         '[pod.epiccastle.bbssh.channel-exec :as channel-exec]
         '[pod.epiccastle.bbssh.input-stream :as input-stream]
         '[pod.epiccastle.bbssh.output-stream :as output-stream])

(defn streams-for-out []
  (let [os (output-stream/new)
        is (input-stream/new os 1024)]
    [os is]))

(let [agent (agent/new)
      session (agent/get-session agent
                                 (System/getenv "USER") ;; username
                                 "localhost"            ;; hostname
                                 22)]                   ;; port
  (session/set-config session :strict-host-key-checking false)
  (session/set-password session "my-password") ;; our password
  (session/connect session)

  (let [channel (session/open-channel session "exec")
        input (input-stream/new)
        [out-stream out-in] (streams-for-out)
        [err-stream err-in] (streams-for-out)]
    (input-stream/close input)
    (channel-exec/set-command channel "id") ;; the command to run
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

 - pod.epiccastle.bbssh.pod.*

     All public functions in these namespaces are exposed in the pod interface and will be called via pod `invoke`. These all run inside the pod native image. They have full access to the bbssh jvm heap.

 - pod.epiccastle.bbssh.*

     All code in these namespaces is injected into the babashka instance apon pod `describe`. These namespaces comprise the API you call when using bbssh from babashka.

## Running tests

You will need `docker` installed to run tests.

By default, the tests run against a JVM version of the pod. This is to avoid having to build the native image during the normal dev cycle.

```
$ make test
```

In order to test against the native-image compiled version of the pod, use:

```
$ make test BBSSH_TEST_TARGET=native-image
```

### Copyright

Copyright (c) Crispin Wellington. All rights reserved.

The use and distribution terms for this software are covered by the
Eclipse Public License 2.0 which can be found in `LICENSE`.
