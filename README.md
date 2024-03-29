# bbssh
Babashka pod for SSH support.

[![CircleCI](https://circleci.com/gh/epiccastle/bbssh/tree/main.svg?style=shield)](https://circleci.com/gh/epiccastle/bbssh/tree/main)
[![Babashka](https://raw.githubusercontent.com/babashka/babashka/master/logo/badge.svg)](https://github.com/babashka/babashka)
[![docs](https://img.shields.io/badge/website-docs-blue)](https://epiccastle.io/bbssh)

## Installing

**The pod should automatically install on first use.**

## Using

Here is a simple script that connects over ssh, the runs a command and disconnects, returning the standard output:

```clojure
(ns test-simple.core
  (:require [babashka.pods :as pods]))

(pods/load-pod 'epiccastle/bbssh "0.5.0")

(require '[pod.epiccastle.bbssh.core :as bbssh])

(-> (bbssh/ssh "remotehost" {:username "remote-user"})
    (bbssh/exec "echo 'I am running remotely'" {:out :string})
    deref
    :out)
```

## API documentation

The full documentation [can be found here.](https://epiccastle.io/bbssh)

## Building

The bbssh pod is distributed as a static binary. Build it with:

```console
$ make
```

This will generate the file `bbssh`.

## Running

### In clojure

```console
$ make run
clj -J-Djava.library.path=resources -m bbssh.core
...
```

### As native image

```console
$ build/bbssh -v
```

## System wide installation

If you would like to install a copy manually, use:

```console
$ curl -O https://raw.githubusercontent.com/epiccastle/bbssh/main/scripts/install
$ bash install
```

And then refer to the pod:

```clojure
(pods/load-pod "bbssh" {:transport :socket})
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
