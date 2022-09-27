# Overview

bbssh is a babashka-pod that provides ssh support to babashka code.

## Requirements

Babashka v0.1.194 or higher is required.

## Installation

The pod artefact is a single static executable. Install this on your
path and making it executable. Alternatively, use the installation script:

When installed you should be able to run it from the shell:

```bash
$ bbssh -v
bbssh version 0.1.1-SNAPSHOT
```

## Quickstart

After installing try writing the following into `test_bbssh.clj`

```clojure
(ns test-bbssh
  (:require [babashka.pods :as pods]))

(pods/load-pod "bbssh" {:transport :socket})

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
