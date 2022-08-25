(ns bbssh.impl.userinfo
  (:require [bbssh.impl.terminal :as terminal]
            [clojure.string :as string])
  (:import [com.jcraft.jsch UserInfo]))

(set! *warn-on-reflection* true)

(defn make-user-info
  "construct a custom Jsch UserInfo instance"
  [{:keys [strict-host-key-checking
           accept-host-key]}]
  (let [state (atom nil)]
    (proxy [UserInfo] []
      (getPassword []
        (if-let [password (terminal/print-flush-ask-no-echo
                           (str "Enter " @state ":"))]
          (do
            (println)
            password)
          (do
            (println "^C")
            (System/exit 1))))

      (promptYesNo [^String s]
        (let [host-key-missing? (and (.contains s "authenticity of host")
                                     (.contains s "can't be established"))
              host-key-changed? (.contains s "IDENTIFICATION HAS CHANGED")]
          (cond
            host-key-missing?
            (let [fingerprint (second (re-find #"fingerprint is ([0-9a-fA-F:]+)." s))]
              (cond
                (not (nil? (#{false "no" :no "n" :n} strict-host-key-checking)))
                true

                (#{true "yes" :yes "y" :y "always" :always} accept-host-key)
                true

                (and (string? accept-host-key)
                     (= (string/lower-case fingerprint)
                        (string/lower-case accept-host-key)))
                true

                accept-host-key
                false

                :else
                (terminal/print-flush-ask-yes-no s)))

            ;; strict-host-key-checking=true:
            ;; showMessage will be called with a refusal to connect

            ;; strict-host-key-checking=false:
            ;; connection will proceed, but key will not be added

            ;; strict-host-key-checking=nil:
            ;; question will be "Do you want to delete the old key and insert the new key?"
            host-key-changed?
            (let [fingerprint (second (re-find #"\n([0-9a-fA-F:]+)." s))]
              (cond
                (#{true "yes" :yes "y" :y "always" :always}
                 accept-host-key)
                true

                (not
                 (nil?
                  (#{false "no" :no "n" :n "never" :never}
                   accept-host-key)))
                false

                (and (string? accept-host-key)
                     (= (string/lower-case fingerprint)
                        (string/lower-case accept-host-key)))
                true

                (string? accept-host-key)
                false

                :else
                (terminal/print-flush-ask-yes-no s)))

            :else
            (terminal/print-flush-ask-yes-no s))))

      (getPassphrase []
        (if-let [passphrase (terminal/print-flush-ask-no-echo
                             (str "Enter " @state ":"))]
          (do
            (println)
            passphrase)
          (do
            (println "^C")
            (System/exit 1))))

      (promptPassphrase [s]
        (reset! state s)
        ;; true decrypt key
        ;; false cancel key decrypt
        true)

      (promptPassword [s]
        (reset! state s)
        ;; return true to continue
        ;; false to cancel auth
        true)

      (showMessage [s]
        (println s)))))
