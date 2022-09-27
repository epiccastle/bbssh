(ns pod.epiccastle.bbssh.ssh-agent
  (:require [pod.epiccastle.bbssh.impl.pack :as pack]
            [pod.epiccastle.bbssh.impl.socket :as socket]
            [pod.epiccastle.bbssh.identity :as identity]
            [pod.epiccastle.bbssh.identity-repository :as identity-repository]))

(def codes
  {:ssh-agent-failure 5
   :request-identities 11
   :identities-answer 12
   :sign-request 13
   :sign-response 14})

(def code->keyword
  (->> codes
       (map reverse)
       (map vec)
       (into {})))

(defn get-sock-path []
  (System/getenv "SSH_AUTH_SOCK"))

(defn read-identity
  "returns [[byte-stream comment] remaining-data]"
  [data]
  (let [[byte-stream data] (pack/decode-string data)
        [comment data] (pack/decode-string data)]
    [[(byte-array byte-stream) (apply str (map char comment))] data]))

(defn decode-identities
  "returns vector of identities. Each identity is [byte-seq comment-string]"
  [data]
  (let [[val data] (split-at 4 data)
        num-identities (pack/unpack-int val)]
    (loop [n num-identities
           data data
           identities []]
      (if (pos? n)
        (let [[id data] (read-identity data)]
          (recur (dec n) data (conj identities id)))
        (do
          (assert (empty? data) "data should be empty now")
          identities)))))

(defn send-query [sock query-data]
  (let [query (concat (pack/pack-int (count query-data)) query-data)
        qarr (byte-array query)]
    (socket/write sock qarr)))

(defn read-response [sock]
  (let [size (pack/unpack-int (socket/read sock 4))]
    (let [data (socket/read sock size)]
      data)))

(defn request-identities [sock]
  (->> :request-identities codes pack/pack-byte
       (send-query sock))
  (let [response (read-response sock)]
    (case (code->keyword (first response))
      :identities-answer
      (decode-identities (drop 1 response))

      :ssh-agent-failure
      (throw (ex-info "ssh-agent failed" {:type ::agent-failed
                                          :response response}))

      (throw (ex-info "ssh-agent failed" {:type ::unknown-response
                                          :response response})))))

(defn sign-request [sock blob data algorithm]
  (->> (concat
        (pack/pack-byte (codes :sign-request))
        (pack/pack-data blob)
        (pack/pack-data data)
        (pack/pack-int (case algorithm
                         "rsa-sha2-256" 0x2
                         "rsa-sha2-512" 0x4
                         0x0)))
       (send-query sock))
  (let [response (read-response sock)]
    (case (code->keyword (first response))
      :sign-response
      (let [[blob data] (pack/decode-string (drop 1 response))]
        (when-not (empty? data)
          (throw (ex-info "ssh-agent failed: trailing data should not exist"
                          {:type ::protocol-error
                           :response response})))
        (byte-array blob))

      :ssh-agent-failure
      (throw (ex-info "ssh-agent failed: couldn't access key"
                      {:type ::key-access-failed
                       :response response}))

      (throw (ex-info "ssh-agent failed"
                      {:type ::unknown-response
                       :response response})))))

(defn new-identity [[blob comment]]
  (identity/new
   {:set-passphrase (fn [bytes] true)
    :get-public-key-blob (fn [] blob)
    :get-signature
    (fn [data algorithm]
      (when-let [auth-sock-path (get-sock-path)]
        (let [sock (socket/open auth-sock-path)
              signature (sign-request sock blob data algorithm)]
          (socket/close sock)
          signature)))
    :get-alg-name
    (fn []
      (->> blob pack/decode-string first (map char) (apply str)))
    :get-name
    (fn [] comment)
    :is-encrypted (fn [] false)
    :clear (fn [] nil)
    }))

(defn new-identity-repository []
  (identity-repository/new
   {:get-name (fn [] "ssh-agent")
    :get-status (fn [] (if (get-sock-path)
                         :running
                         :unavailable))
    :get-identities
    (fn []
      (when-let [auth-sock-path (get-sock-path)]
        (let [sock (socket/open auth-sock-path)
              identities (request-identities sock)]
          (socket/close sock)
          (let [result (mapv new-identity identities)]
            result))))}))
