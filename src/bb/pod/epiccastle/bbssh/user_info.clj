(ns pod.epiccastle.bbssh.user-info
  (:require [pod.epiccastle.bbssh.utils :as utils]))

(defn new
  "Create a new user-info structure. Pass in a hashmap containing the
  functions to execute as values. These functions will be called
  by the internal ssh engine. The hashmap should contain some
  subset of the following keywords:

  ```clojure
  :get-password (fn [] ...)
  ;; return the password for the user

  :get-passphrase (fn [] ...)
  ;; return the passphrase for the user

  :prompt-yes-no (fn [^String question] ...)
  ;; ask the user a yes/no question. Block until the answer is
  ;; gathered. Return a truthy value if yes.

  :prompt-passphrase (fn [^String question] ...)
  ;; Ask the user for their passphrase. Block until it is gathered.
  ;; Return true to continue connecting or false to quit the
  ;; connection. Note: do not return the passphrase. That is gathered
  ;; with a subsequent :get-passphrase if this function returned true.

  :prompt-password (fn [^String question] ...)
  ;; Ask the user for their password. Block until it is gathered.
  ;; Return true to continue connecting or false to quit the
  ;; connection. Note: do not return the password. That is gathered
  ;; with a subsequent :get-password if this function returned true.

  :show-message (fn [^String message] ...)
  ;; Display for the user the specified message.
  ```
  "
  [callbacks]
  (utils/new-invoker
   {:call-sym 'pod.epiccastle.bbssh.impl.user-info/new
    :args []
    :callbacks callbacks}))
