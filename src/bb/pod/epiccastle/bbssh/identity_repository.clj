(ns pod.epiccastle.bbssh.identity-repository
  (:require [pod.epiccastle.bbssh.impl.identity-repository :as identity-repository]
            [pod.epiccastle.bbssh.impl.callbacks :as callbacks]
            [pod.epiccastle.bbssh.cleaner :as cleaner]
            [pod.epiccastle.bbssh.utils :as utils]))

(defn- preprocess-args [method args]
  (case method
    :add
    (let [[data] args]
      [(utils/decode-base64 data)])

    :remove
    (let [[data] args]
      [(utils/decode-base64 data)])

    args))

(defn- postprocess-returns [method result]
  (case method
    :get-identities
    (mapv cleaner/split-key result)

    result))

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
                 f (callbacks method (fn [& args]))]
             ;; without the future here I get into a deadlock
             ;; within bb/sci. The babashka.pods.impl/processor
             ;; function gets blocked forever by the success
             ;; callback
             ;; https://github.com/babashka/pods/blob/eb0b01c0a69cf7ef24b0277d4449a157253a3037/src/babashka/pods/impl.clj#L231
             ;; and then it can't process any more async
             ;; responses.
             (future
               (->> args
                    (preprocess-args method)
                    (apply f)
                    (postprocess-returns method)
                    (callbacks/return-result id))))

           ;; final message apon deletion triggers this response
           :done
           nil))
       :error (fn [err]
                (prn 'error err))}})
    (cleaner/register @p)))
