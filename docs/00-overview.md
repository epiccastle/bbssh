# Overview

bbssh is a babashka-pod that provides ssh support to babashka code.

Project repository is [here](https://github.com/epiccastle/bbssh)

## Requirements

Babashka v0.10.194 or higher is required for full functionality.

## Installation

The pod artefact is a single static executable. Install this on your
path and making it executable. Alternatively, use the installation script:

```bash
$ curl -O https://raw.githubusercontent.com/epiccastle/bbssh/main/scripts/install
$ bash install
```

When installed you should be able to run it from the shell:

```bash
$ bbssh -v
bbssh version 0.1.0
```

## Quickstart

After installing try writing the following into `test_bbssh.clj`

```clojure
(ns test-bbssh
  (:require [babashka.pods :as pods]))

(pods/load-pod 'epiccastle/bbssh "0.1.0")

(require '[pod.epiccastle.bbssh.core :as bbssh])

(let [session (bbssh/ssh "localhost")]
  (-> (bbssh/exec session "echo 'I am running over ssh'" {:out :string})
      deref
      :out))
```

Then execute the file with babashka. You will be prompted for your ssh password. Enter it and press return:

```bash-shell
$ bb test_bbssh.clj
Enter Password for crispin@localhost:
"I am running over ssh\n"
```

> **Note:** if you are running an ssh-agent and you have a relevant key you may not be asked for your password. bbssh supports authentication by ssh agent.

## Copyright

Copyright (c) Crispin Wellington. All rights reserved.

The use and distribution terms for this software are covered by the
Eclipse Public License 2.0 which can be found in `LICENSE`.
