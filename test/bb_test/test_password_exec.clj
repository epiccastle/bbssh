(ns bb-test.test-password-exec
  (:require [pod.epiccastle.bbssh.core :as bbssh]
            [pod.epiccastle.bbssh.agent :as agent]
            [pod.epiccastle.bbssh.session :as session]
            [pod.epiccastle.bbssh.channel-exec :as channel-exec]
            [pod.epiccastle.bbssh.input-stream :as input-stream]
            [pod.epiccastle.bbssh.output-stream :as output-stream]
            [bb-test.docker :as docker]
            [clojure.test :refer [is deftest]]
            [clojure.string :as string]
            [clojure.java.io :as io]))

(defn streams-for-out []
  (let [os (output-stream/new)
        is (input-stream/new os 1024)]
    [os is]))

(deftest password-exec-low-level
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})

  (let [{:keys [username hostname port password]}
        {:username "root"
         :password "root-access-please"
         :hostname "localhost"
         :port 9876}
        ]
    (let [agent (agent/new)
          session (agent/get-session agent username hostname port)]
      (session/set-password session password)
      (session/set-config session :strict-host-key-checking false)
      (session/connect session)
      (assert (session/connected? session))
      (let [channel (session/open-channel session "exec")
            input-stream (input-stream/new)
            [out-stream out-in] (streams-for-out)
            [err-stream err-in] (streams-for-out)
            ]
        (channel-exec/set-command channel "id")
        (channel-exec/set-input-stream channel input-stream false)
        (input-stream/close input-stream)

        (channel-exec/set-output-stream channel out-stream)
        (channel-exec/set-error-stream channel err-stream)

        (channel-exec/connect channel)
        (let [buff (byte-array 1024)
              num (input-stream/read out-in buff 0 1024)
              result (-> (java.util.Arrays/copyOfRange buff 0 num)
                         (String. "UTF-8")
                         )]
          (is (string/starts-with? result "uid=0(root) gid=0(root)"))))))
  (docker/cleanup))

(deftest exec-in-all-types
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})
  (let [opts {:username "root"
              :password "root-access-please"
              :port 9876
              :strict-host-key-checking false}
        session (bbssh/ssh "localhost" opts)]

    ;; :in nil
    (let [{:keys [channel out err in]}
          (bbssh/exec session "cat" {:in nil})]
      (is (= 0 (.available out)))
      (is (channel-exec/is-closed channel))
      (is (= 0 (channel-exec/get-exit-status channel))))

    ;; :in string
    (let [{:keys [channel out err in]}
          (bbssh/exec session "cat" {:in "string\ninput\n"})]
      (is (= "string\ninput\n"
             (with-out-str (io/copy out *out*))))
      (is (channel-exec/is-closed channel))
      (is (= 0 (channel-exec/get-exit-status channel))))

    ;; :in string different encoding
    (let [{:keys [channel out err in]}
          (bbssh/exec session "cat" {:in "🚢"
                                     :in-enc "utf-16"})]
      (is (= "🚢"
             (with-out-str
               (io/copy out *out* :encoding "utf-16"))))
      (is (channel-exec/is-closed channel))
      (is (= 0 (channel-exec/get-exit-status channel))))

    ;; :in byte-array
    (let [{:keys [channel out err in]}
          (bbssh/exec session "cat" {:in (.getBytes "string\ninput\n")})]
      (is (= "string\ninput\n"
             (with-out-str (io/copy out *out*))))
      (is (channel-exec/is-closed channel))
      (is (= 0 (channel-exec/get-exit-status channel))))

    ;; :in byte-array different encoding
    (let [{:keys [channel out err in]}
          (bbssh/exec session "cat" {:in (.getBytes "🚢" "utf-16")})]
      (is (= "🚢"
             (with-out-str (io/copy out *out* :encoding "utf-16"))))
      (is (channel-exec/is-closed channel))
      (is (= 0 (channel-exec/get-exit-status channel))))

    ;; :in InputStream
    (let [{:keys [channel out err in]}
          (bbssh/exec
           session "cat"
           {:in
            (java.io.ByteArrayInputStream.
             (byte-array (range 128)))})]
      (let [buff (byte-array 256)]
        (is (= 128 (.read out buff 0 256)))
        (is (-> (java.util.Arrays/copyOfRange buff 0 128)
                seq
                (= (range 128)))))
      (is (channel-exec/is-closed channel))
      (is (= 0 (channel-exec/get-exit-status channel))))

    ;; :in pod side input-stream
    (let [in-output-stream (output-stream/new)
          in-stream (input-stream/new in-output-stream)
          {:keys [channel out err in]}
          (bbssh/exec
           session "cat"
           {:in in-stream})]
      (doseq [n (range 128)]
        (output-stream/write in-output-stream n))
      (output-stream/close in-output-stream)
      (let [buff (byte-array 256)]
        (is (= 128 (.read out buff 0 256)))
        (is (-> (java.util.Arrays/copyOfRange buff 0 128)
                seq
                (= (range 128)))))
      (is (channel-exec/is-closed channel))
      (is (= 0 (channel-exec/get-exit-status channel))))

    ;; :in :stream
    (let [{:keys [channel out err in]}
          (bbssh/exec
           session "cat"
           {:in :stream})]
      (doseq [n (range 128)]
        (.write in n))
      (.close in)
      (let [buff (byte-array 256)]
        (is (= 128 (.read out buff 0 256)))
        (is (-> (java.util.Arrays/copyOfRange buff 0 128)
                seq
                (= (range 128)))))
      (is (channel-exec/is-closed channel))
      (is (= 0 (channel-exec/get-exit-status channel)))))

  (docker/cleanup))

(deftest test-wait
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})

  (let [opts {:username "root"
              :password "root-access-please"
              :port 9876
              :strict-host-key-checking false}
        session (bbssh/ssh "localhost" opts)]
    (let [{:keys [channel] :as process}
          (bbssh/exec session "sleep 0.5" {:in nil})]
      (is (nil? (channel-exec/wait channel 0)))
      (is (nil? (channel-exec/wait channel -1)))
      (is (nil? (channel-exec/wait channel 100)))
      (is (channel-exec/is-connected channel))
      (is (= 0 (channel-exec/wait channel 10000)))
      (is (not (channel-exec/is-connected channel))))

    (let [{:keys [channel]} (bbssh/exec session "sleep 0.5" {:in nil})]
      (is (= 0 (channel-exec/wait channel))))

    (let [{:keys [channel]} (bbssh/exec session "sleep 0.5; exit 10" {:in nil})]
      (is (= 10 (channel-exec/wait channel)))))

  (docker/cleanup))

(deftest test-deref
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})

  (let [opts {:username "root"
              :password "root-access-please"
              :port 9876
              :strict-host-key-checking false}
        session (bbssh/ssh "localhost" opts)]
    (let [{:keys [channel exit] :as process}
          (-> (bbssh/exec session "sleep 0.5" {:in nil})
              deref)]
      (is (not (channel-exec/is-connected channel)))
      (is (= 0 exit)))
    (let [{:keys [channel exit] :as process}
          (-> (bbssh/exec session "sleep 0.5; exit 10" {:in nil})
              deref)]
      (is (not (channel-exec/is-connected channel)))
      (is (= 10 exit)))
    (let [{:keys [channel exit out err] :as process}
          @(bbssh/exec session
                       "sleep 0.5; echo foo; echo bar 1>&2; exit 10"
                       {:in nil
                        :out :string
                        :err :string})]
      (is (not (channel-exec/is-connected channel)))
      (is (= 10 exit))
      (is (= "foo\n" out))
      (is (= "bar\n" err)))
    (let [{:keys [channel exit out err] :as process}
          @(bbssh/exec session
                       "sleep 0.5; echo foo; echo bar 1>&2; exit 10"
                       {:in nil
                        :out :bytes
                        :err :bytes})]
      (is (not (channel-exec/is-connected channel)))
      (is (= 10 exit))
      (is (bytes? out))
      (is (= (seq out) '(102 111 111 10)))
      (is (bytes? err))
      (is (= (seq err) '(98 97 114 10)))))

  (docker/cleanup))




#_
(deftest exec-in-nil
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})

  (docker/cleanup))
