(ns bb-test.test-proxy-support
  (:require [pod.epiccastle.bbssh.core :as bbssh]
            [pod.epiccastle.bbssh.agent :as agent]
            [clojure.string :as string]
            [pod.epiccastle.bbssh.session :as session]
            [pod.epiccastle.bbssh.key-pair :as key-pair]
            [pod.epiccastle.bbssh.config-repository :as config-repository]
            [babashka.process :as process]
            [bb-test.docker :as docker]
            [clojure.test :refer [is deftest testing]]
            [clojure.java.io :as io]
            [clj-commons.digest :as digest])
  (:import [java.io File BufferedInputStream]))

;; The ports we'll run the proxy server on. Note they are port numbers within
;; the container so no worries that it would conflict with any host service that
;; may use these ports.
(def proxy-no-auth-port 1080)
(def proxy-auth-port 2080)

;; Proxy auth credentials we use for testing.
(def proxy-username "bbssh-test-proxy-username")
(def proxy-password "bbssh-test-proxy-password")

(defn create-session
  [& {:keys [host port proxy]}]
  (bbssh/ssh host
             (cond-> {:port port
                      :username "root"
                      :password "root-access-please"
                      :strict-host-key-checking false}
               proxy
               (assoc :proxy proxy))))

(defn stop-proxy
  []
  (docker/exec "sh -c 'pkill gost || true'"))

(defn make-proxy-listen-arg
  [proto]
  (let [proto (name proto)
        no-auth-listen-arg (format "-L %s://:%s"
                                   proto
                                   proxy-no-auth-port)
        auth-listen-arg (format "-L %s://%s:%s@:%s"
                                proto
                                proxy-username
                                proxy-password
                                proxy-auth-port)]
    (format "%s %s" no-auth-listen-arg auth-listen-arg)))

(defn start-proxy
  "Start a proxy server with all given protocol support."
  [proto]
  (stop-proxy)
  (let [listen-arg (make-proxy-listen-arg proto)
        ;; Nohup is required so the docker exec could exit but keep the cmd
        ;; running in the background.
        cmd (format "sh -c 'nohup /usr/local/bin/gost %s &'" listen-arg)]
    #_(println (format "Launch gost proxy server for %s protocol:\n%s\n" proto cmd))
    (docker/exec cmd)))

(deftest test-openssh-config
  (docker/cleanup)
  (docker/build {:root-password "root-access-please"})
  (docker/start {:ssh-port 9876})

  (doseq [proxy-type [:http :socks4 :socks5]]
    (testing (str "proxy type - " proxy-type)
      (stop-proxy)
      (let [container-ip (docker/get-container-ip)]
        (is (create-session :host "localhost"
                            :port 9876))
        (is
          (thrown? clojure.lang.ExceptionInfo
                   (create-session :host "localhost"
                                   :port 9876
                                   :proxy {:type proxy-type
                                           :host container-ip
                                           :port proxy-no-auth-port})))
        (start-proxy proxy-type)
        (is
          (create-session :host container-ip
                          ;; The proxy server runs within the container, so from
                          ;; its
                          ;; view the test ssh server listens on port 22.
                          :port 22
                          :proxy {:type proxy-type
                                  :host container-ip
                                  :port proxy-no-auth-port}))

        ;; socks4 doesn't support authentication
        ;; https://www.openssh.com/txt/socks4.protocol
        (when (not= proxy-type :socks4)
          (testing (str "&& missing proxy credentials - " proxy-type)
            (is
              (thrown? clojure.lang.ExceptionInfo
                       (create-session :host "localhost"
                                       :port 22
                                       :proxy {:type proxy-type
                                               :host container-ip
                                               :port proxy-auth-port}))))

          (testing (str "&& wrong proxy credentials - " proxy-type)
            (is
              (thrown? clojure.lang.ExceptionInfo
                       (create-session :host "localhost"
                                       :port 22
                                       :proxy {:type proxy-type
                                               :host container-ip
                                               :port proxy-auth-port
                                               :username "foo"
                                               :password "bar"}))))

          (testing (str "&& correct proxy credentials - " proxy-type)
            (is
              (create-session :host "localhost"
                              :port 22
                              :proxy {:type proxy-type
                                      :host container-ip
                                      :port proxy-auth-port
                                      :username proxy-username
                                      :password proxy-password}))))
      ))))
