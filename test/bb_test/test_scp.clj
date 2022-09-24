(ns bb-test.test-scp
  (:require [pod.epiccastle.bbssh.core :as bbssh]
            [pod.epiccastle.bbssh.agent :as agent]
            [pod.epiccastle.bbssh.session :as session]
            [pod.epiccastle.bbssh.scp :as scp]
            [pod.epiccastle.bbssh.channel-exec :as channel-exec]
            [pod.epiccastle.bbssh.input-stream :as input-stream]
            [pod.epiccastle.bbssh.output-stream :as output-stream]
            [babashka.process :as process]
            [bb-test.docker :as docker]
            [clojure.test :refer [is deftest]]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [clj-commons.digest :as digest]))

(deftest exec-scp
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})

  (let [session (bbssh/ssh "localhost" {:port 9876
                                        :username "root"
                                        :password "root-access-please"
                                        :strict-host-key-checking false})]
    #_(docker/exec "mkdir /root/bbssh-test")

    (scp/scp-to [(io/file "deps.edn")] "bbssh-test"
                {:session session})
    (is (-> (docker/exec "md5sum /root/bbssh-test")
             (string/split #" ")
             first
             (= (digest/md5 (io/as-file "deps.edn")))))



    )

  (docker/cleanup))
