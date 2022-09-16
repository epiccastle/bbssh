(ns pod.epiccastle.bbssh.utils
  (:require [pod.epiccastle.bbssh.impl.callbacks :as callbacks]
            [pod.epiccastle.bbssh.cleaner :as cleaner]))

(def decoder (java.util.Base64/getDecoder))

(defn decode-base64 [base64]
  (.decode decoder base64))

(def encoder (java.util.Base64/getEncoder))

(defn encode-base64 [array]
  (.encodeToString encoder array))

(defn opt-decode-base64
  "If passed a string, decode it as base64 and return a byte array.
  If passed a byte array, just return it as is."
  [key]
  (if (string? key)
    (decode-base64 key)
    key))

(defn opt-get-bytes
  "If passed a string, return the underlying byte array using the
  encoding (defaults to utf8). If passed a byte arrayu, just return it
  as it is."
  [data & [encoding]]
  (if (string? data)
    (.getBytes data (or encoding "utf-8"))
    data))

(defn new-invoker [{:keys [call-sym args
                           callbacks
                           preprocess-args-fn
                           postprocess-returns-fn
                           ]
                    :or {preprocess-args-fn (fn [_method args] args)
                         postprocess-returns-fn (fn [_method result] result)}}]
  (let [p (promise)]
    (babashka.pods/invoke
     "pod.epiccastle.bbssh"
     call-sym
     args
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
             ;; without the future here I get into a deadlock
             ;; within bb/sci. The babashka.pods.impl/processor
             ;; function gets blocked forever by the success
             ;; callback
             ;; https://github.com/babashka/pods/blob/eb0b01c0a69cf7ef24b0277d4449a157253a3037/src/babashka/pods/impl.clj#L231
             ;; and then it can't process any more async
             ;; responses.
             (future
               (->> args
                    (preprocess-args-fn method)
                    (apply f)
                    (postprocess-returns-fn method)
                    (callbacks/return-result id))))

           ;; final message apon deletion triggers this response
           :done
           nil))
       :error (fn [err]
                (prn 'error err))}})
    (cleaner/register @p)))
