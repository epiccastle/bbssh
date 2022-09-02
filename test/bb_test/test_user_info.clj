(ns bb-test.test-user-info
  (:require [pod.epiccastle.bbssh.user-info :as user-info]
            [pod.epiccastle.bbssh.agent :as agent]
            [pod.epiccastle.bbssh.session :as session]
            [bb-test.docker :as docker]
            [clojure.test :refer [is deftest]]
            [clojure.string :as string]))

(deftest user-info
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})
  (let [{:keys [username hostname port password]}
        {:username "root"
         :password "root-access-please"
         :hostname "localhost"
         :port 9876}
        user-info (user-info/new
                   {:prompt-password (fn [_] true)
                    :get-password (fn [] password)})
        agent (agent/new)
        session (agent/get-session agent username hostname port)]
    (session/set-user-info session user-info)
    (session/set-config session :strict-host-key-checking false)
    (session/connect session)
    (is (session/connected? session))
    (session/disconnect session)

    (let [user-info (user-info/new
                     {:prompt-password (fn [_] false)})
          session (agent/get-session agent username hostname port)
          ]
      (session/set-config session :strict-host-key-checking false)
      (session/set-user-info session user-info)
      (is
       (thrown? clojure.lang.ExceptionInfo
                (session/connect session))))

    (let [user-info (user-info/new
                     {:prompt-password (fn [_] true)
                      :get-password (fn [] "wrong-password")})
          session (agent/get-session agent username hostname port)
          ]
      (session/set-config session :strict-host-key-checking false)
      (session/set-user-info session user-info)
      (is
       (thrown? clojure.lang.ExceptionInfo
                (session/connect session)))))

  (docker/cleanup))
