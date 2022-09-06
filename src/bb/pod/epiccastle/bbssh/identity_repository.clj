(ns pod.epiccastle.bbssh.identity-repository
  (:require [pod.epiccastle.bbssh.impl.identity-repository :as identity-repository]
            [pod.epiccastle.bbssh.impl.callbacks :as callbacks]
            [pod.epiccastle.bbssh.cleaner :as cleaner]))

(defn new [callbacks]
  (let [p (promise)]
    (babashka.pods/invoke
     "pod.epiccastle.bbssh"
     'pod.epiccastle.bbssh.impl.identity-repository/new
     []
     {:handlers
      {:success
       (fn [[method value]]
         (case method

           ;; the returned object
           :result
           (deliver p value)

           ;; a callback message
           :method
           (let [{:keys [id method args]} value
                 f (callbacks method (fn [& _args]))]
             ;; without the future here I get into a deadlock within
             ;; bb/sci. The babashka.pods.impl/processor
             ;; function gets blocked forever by the success
             ;; callback
             ;; https://github.com/babashka/pods/blob/eb0b01c0a69cf7ef24b0277d4449a157253a3037/src/babashka/pods/impl.clj#L231
             ;; and then it can't process any more async
             ;; responses.
             (future
               (callbacks/return-result id (apply f args))))

           ;; final message apon deletion triggers this response
           :done
           nil))
       :error (fn [err]
                (prn 'error err))}})
    (cleaner/register @p)))
