(ns bb-test.test-openssh-config
  (:require [pod.epiccastle.bbssh.core :as bbssh]
            [pod.epiccastle.bbssh.agent :as agent]
            [pod.epiccastle.bbssh.session :as session]
            [pod.epiccastle.bbssh.key-pair :as key-pair]
            [pod.epiccastle.bbssh.config-repository :as config-repository]
            [babashka.process :as process]
            [bb-test.docker :as docker]
            [clojure.test :refer [is deftest]]
            [clojure.java.io :as io]
            [clj-commons.digest :as digest])
  (:import [java.io File BufferedInputStream]))

(deftest test-openssh-config
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})

  (let [agent (agent/new)
        config (config-repository/openssh-config-string "
Port 9876

Host docker-host
  User root
  Hostname localhost

Host *
  ConnectTime 30000
  PreferredAuthentications keyboard-interactive,password,publickey
  #ForwardAgent yes
  #StrictHostKeyChecking no
  #IdentityFile ~/.ssh/id_rsa
  #UserKnownHostsFile ~/.ssh/known_hosts
")]
    (agent/set-config-repository agent config)

    (let [session (agent/get-session agent "docker-host")]
      (session/set-password session "root-access-please")
      (session/set-config session :strict-host-key-checking false)
      (session/connect session)
      (let [{:keys [exit out]} @(bbssh/exec session "echo test" {:out :string})]
        (is (zero? exit))
        (is (= "test\n" out)))))

  (docker/cleanup))

(deftest test-openssh-config-file
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})

  (spit "/tmp/bbssh-config" "
Port 9876

Host docker-host
  User root
  Hostname localhost

Host *
  ConnectTime 30000
  PreferredAuthentications keyboard-interactive,password,publickey
  #ForwardAgent yes
  #StrictHostKeyChecking no
  #IdentityFile ~/.ssh/id_rsa
  #UserKnownHostsFile ~/.ssh/known_hosts
")

  (let [agent (agent/new)
        config (config-repository/openssh-config-file "/tmp/bbssh-config")]
    (agent/set-config-repository agent config)

    (let [session (agent/get-session agent "docker-host")]
      (session/set-password session "root-access-please")
      (session/set-config session :strict-host-key-checking false)
      (session/connect session)
      (let [{:keys [exit out]} @(bbssh/exec session "echo test" {:out :string})]
        (is (zero? exit))
        (is (= "test\n" out)))))

  (docker/cleanup))

(deftest test-openssh-config-key-auth-ecdsa
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})

  (let [agent (agent/new)
        keypair (key-pair/generate agent :ecdsa 256)]
    (key-pair/write-private-key keypair "/tmp/bbssh_id_ecdsa")
    (key-pair/write-public-key keypair "/tmp/bbssh_id_ecdsa.pub" "docker-key")

    (process/sh "chmod 0700 /tmp/bbssh_id_rsa")
    (docker/exec "mkdir /root/.ssh")
    (docker/exec "chmod 0700 /root/.ssh")
    (docker/cp-to "/tmp/bbssh_id_ecdsa.pub" "/root/.ssh/authorized_keys")
    (docker/exec "chmod 0600 /root/.ssh/authorized_keys")
    (docker/exec "chown root:root -R /root/.ssh")

    (let [config (config-repository/openssh-config-string "
Port 9876

Host docker-host
  User root
  Hostname localhost

Host *
  ConnectTime 30000
  PreferredAuthentications publickey
  IdentityFile /tmp/bbssh_id_ecdsa
  UserKnownHostsFile /tmp/known_hosts
")]
      (agent/set-config-repository agent config)

      (let [session (agent/get-session agent "docker-host")]
        (session/set-config session :strict-host-key-checking false)
        (session/connect session)
        (let [{:keys [exit out]} @(bbssh/exec session "echo test" {:out :string})]
          (is (zero? exit))
          (is (= "test\n" out))))))

  (docker/cleanup))

(deftest test-openssh-config-key-auth-rsa
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})

  (let [agent (agent/new)
        keypair (key-pair/generate agent :rsa 1024)]
    (key-pair/write-private-key keypair "/tmp/bbssh_id_rsa")
    (key-pair/write-public-key keypair "/tmp/bbssh_id_rsa.pub" "docker-key")

    (process/sh "chmod 0700 /tmp/bbssh_id_rsa")
    (docker/exec "mkdir /root/.ssh")
    (docker/exec "chmod 0700 /root/.ssh")
    (docker/cp-to "/tmp/bbssh_id_rsa.pub" "/root/.ssh/authorized_keys")
    (docker/exec "chmod 0600 /root/.ssh/authorized_keys")
    (docker/exec "chown root:root -R /root/.ssh")

    (let [config (config-repository/openssh-config-string "
Port 9876

Host docker-host
  User root
  Hostname localhost

Host *
  ConnectTime 30000
  PreferredAuthentications publickey
  IdentityFile /tmp/bbssh_id_rsa
  UserKnownHostsFile /tmp/known_hosts
")]
      (agent/set-config-repository agent config)

      (let [session (agent/get-session agent "docker-host")]
        (session/set-config session :strict-host-key-checking false)
        (session/connect session)
        (let [{:keys [exit out]} @(bbssh/exec session "echo test" {:out :string})]
          (is (zero? exit))
          (is (= "test\n" out))))))

  (docker/cleanup))
