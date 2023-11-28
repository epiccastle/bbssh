(ns bb-test.test-garbage-collection
  (:require [pod.epiccastle.bbssh.user-info :as user-info]
            [pod.epiccastle.bbssh.agent :as agent]
            [pod.epiccastle.bbssh.session :as session]
            [pod.epiccastle.bbssh.pod.cleaner :as cleaner]
            [bb-test.docker :as docker]
            [clojure.test :refer [is deftest]])
  (:import [java.lang.ref WeakReference]))

(defn connect [{:keys [username hostname port password]}]
  (let [user-info (user-info/new
                   {:prompt-password (fn [_] true)
                    :get-password (fn [] password)})
        agent (agent/new)
        session (agent/get-session agent username hostname port)
        ]
    (session/set-user-info session user-info)
    (session/set-config session :strict-host-key-checking false)
    (session/connect session)
    (is (session/connected? session))
    (is (not (empty? (cleaner/get-references))))
    (session/disconnect session)
    nil))

(defn gc []
  (let [t (atom (Object.))
        wr (WeakReference. @t)]
    (reset! t nil)
    (while (.get wr)
      (System/gc)
      (System/runFinalization))))

;; the following test passes on my local workstation, but fails on CircleCI
#_(deftest user-info
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})
  (connect
   {:username "root"
    :password "root-access-please"
    :hostname "localhost"
    :port 9876})
  (gc)

  ;; takes some time for the references to be removed
  ;; from the pod. TODO: investigate why its slow
  #_(Thread/sleep 5000)

  (loop [n 0]
    (when (seq (cleaner/get-references))
      (Thread/sleep 500)
      (when (< n 10)
        (recur (inc n)))))

  (is (empty? (cleaner/get-references)))

  (docker/cleanup))
