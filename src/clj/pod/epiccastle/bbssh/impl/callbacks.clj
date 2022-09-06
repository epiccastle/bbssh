(ns pod.epiccastle.bbssh.impl.callbacks)

(def result-register (atom {}))

(defn return-result [id result]
  (let [p (@result-register id)]
    (deliver p result)
    nil))

(defn call-method [reply-fn method args]
  (let [id (str (gensym "callback-"))
        p (promise)]
    (swap! result-register assoc id p)
    (reply-fn [:method {:id id
                        :method method
                        :args args}])
    (let [res @p]
      (swap! result-register dissoc id)
      res)))
