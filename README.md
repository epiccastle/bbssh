# bbssh
Babashka pod for SSH support.

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
   All code in these namespaces is injected into the babashka instance apon pod `describe`
