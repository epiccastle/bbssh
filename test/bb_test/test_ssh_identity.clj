(ns bb-test.test-ssh-identity
  (:require [pod.epiccastle.bbssh.core :as bbssh]
            [pod.epiccastle.bbssh.user-info :as user-info]
            [bb-test.docker :as docker]
            [bb-test.keys :as keys]
            [clojure.test :refer [is deftest]]))

(defn setup-server-client-keys [key-name]
  ;; setup ssh server key
  (docker/exec "mkdir /root/.ssh")
  (docker/exec "chmod 0700 /root/.ssh")
  (docker/put-file (get-in keys/keys [key-name :public]) "/root/.ssh/authorized_keys")

  ;; setup client private key
  (docker/run "rm -rf .test/bbssh-test-key" "could not clean .test/bbssh-test-key")
  (docker/run "mkdir -p .test/bbssh-test-key" "could not mkdir .test/bbssh-test-key")
  (docker/run "chmod 0700 .test/bbssh-test-key" "could not chmod .test/bbssh-test-key")
  (spit ".test/bbssh-test-key/bbssh_test_id_key" (get-in keys/keys [key-name :private]) )
  (docker/run "chmod 0600 .test/bbssh-test-key/bbssh_test_id_key" "could not chmod .test/bbssh-test-key/bbssh_test_id_key"))

(defn passphrase [key-name]
  (get-in keys/keys [key-name :passphrase]))

(deftest test-ssh-via-identity-no-passphrase
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})
  (setup-server-client-keys :rsa-nopassphrase)

  (-> (bbssh/ssh "localhost" {:port 9876
                              :username "root"
                              :identity ".test/bbssh-test-key/bbssh_test_id_key"
                              :strict-host-key-checking false})
      (bbssh/exec "echo 'running remote'" {:out :string})
      deref
      :out
      (= "running remote\n")
      is)

  (docker/cleanup))

(deftest test-ssh-via-identity-with-passphrase
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})
  (setup-server-client-keys :rsa-passphrase)

  (-> (bbssh/ssh "localhost" {:port 9876
                              :username "root"
                              :identity ".test/bbssh-test-key/bbssh_test_id_key"
                              :passphrase (passphrase :rsa-passphrase)
                              :strict-host-key-checking false})
      (bbssh/exec "echo 'running remote'" {:out :string})
      deref
      :out
      (= "running remote\n")
      is)

  (docker/cleanup))

(deftest test-ssh-via-identity-with-missing-passphrase-user-info-is-called
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})
  (setup-server-client-keys :rsa-passphrase)

  ;; ensure passphrase is asked for is key is encrypted and no passphrase given
  (let [state-asked? (atom false)]
    (-> (bbssh/ssh "localhost" {:port 9876
                                :username "root"
                                :identity ".test/bbssh-test-key/bbssh_test_id_key"
                                :strict-host-key-checking false
                                :user-info
                                (user-info/new
                                  {:prompt-passphrase (fn [_]
                                                        (reset! state-asked? true)
                                                        true)
                                   :get-passphrase #(passphrase :rsa-passphrase)})})
        (bbssh/exec "echo 'running remote'" {:out :string})
        deref
        :out
        (= "running remote\n")
        is)
    (is @state-asked?))

  ;; ensure connection fails if passphrase is asked for and wrong passphrase given
  ;; and then wrong password is given
  (->> (-> (bbssh/ssh "localhost" {:port 9876
                                   :username "root"
                                   :identity ".test/bbssh-test-key/bbssh_test_id_key"
                                   :strict-host-key-checking false
                                   :user-info
                                   (user-info/new
                                     {:get-password (fn [] "wrong password")
                                      :prompt-yes-no (fn [_] true)
                                      :prompt-password (fn [_] true)
                                      :show-message (fn [_] nil)
                                      :prompt-passphrase (fn [_] true)
                                      :get-passphrase (fn [] "wrong passphrase")})})
           (bbssh/exec "echo 'running remote'" {:out :string})
           deref)
       (thrown? clojure.lang.ExceptionInfo)
       is)

  ;; ensure connection fails if passphrase falls back to password, but password
  ;; auth is cancelled
  (->> (-> (bbssh/ssh "localhost" {:port 9876
                                   :username "root"
                                   :identity ".test/bbssh-test-key/bbssh_test_id_key"
                                   :strict-host-key-checking false
                                   :user-info
                                   (user-info/new
                                     {:get-password (fn [] "wrong password")
                                      :prompt-yes-no (fn [_] true)
                                      :prompt-password (fn [_]
                                                         ;; cancel password auth
                                                         false)
                                      :show-message (fn [_] nil)
                                      :prompt-passphrase (fn [_] true)
                                      :get-passphrase (fn [] "wrong passphrase")})})
           (bbssh/exec "echo 'running remote'" {:out :string})
           deref)
       (thrown? clojure.lang.ExceptionInfo)
       is)

  ;; ensure connection fails if passphrase decryption is denied
  (->> (-> (bbssh/ssh "localhost" {:port 9876
                                   :username "root"
                                   :identity ".test/bbssh-test-key/bbssh_test_id_key"
                                   :strict-host-key-checking false
                                   :user-info
                                   (user-info/new
                                     {:get-password (fn [] "wrong password")
                                      :prompt-yes-no (fn [_] true)
                                      :prompt-password (fn [_] true)
                                      :show-message (fn [_] nil)
                                      :prompt-passphrase (fn [_]
                                                           ;; cancel decrypting passphrase
                                                           false)
                                      :get-passphrase (fn [] "wrong passphrase")})})
           (bbssh/exec "echo 'running remote'" {:out :string})
           deref)
       (thrown? clojure.lang.ExceptionInfo)
       is)

  (docker/cleanup))

(deftest test-ssh-via-identity-ed25519-no-passphrase
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})
  (setup-server-client-keys :ed25519-no-passphrase)

  (-> (bbssh/ssh "localhost" {:port 9876
                              :username "root"
                              :identity ".test/bbssh-test-key/bbssh_test_id_key"
                              :strict-host-key-checking false})
      (bbssh/exec "echo 'running remote'" {:out :string})
      deref
      :out
      (= "running remote\n")
      is)

  (docker/cleanup))

(deftest test-ssh-via-identity-ed25519-with-passphrase
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})
  (setup-server-client-keys :ed25519-passphrase)

  (-> (bbssh/ssh "localhost" {:port 9876
                              :username "root"
                              :identity ".test/bbssh-test-key/bbssh_test_id_key"
                              :passphrase (passphrase :ed25519-passphrase)
                              :strict-host-key-checking false})
      (bbssh/exec "echo 'running remote'" {:out :string})
      deref
      :out
      (= "running remote\n")
      is)

  (docker/cleanup))
