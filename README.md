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
