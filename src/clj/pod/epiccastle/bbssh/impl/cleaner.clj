(ns pod.epiccastle.bbssh.impl.cleaner
  (:require [bbssh.impl.references :as references]
            [clojure.set :as set]))

(def delete-fns (atom {}))

(defonce _watch
  (add-watch delete-fns :deleter
             (fn [_ _ o n]
               ;; when a key is dissoced
               (doseq [k (set/difference
                          (into #{} (keys o))
                          (into #{} (keys n)))]
                 ;; trigger the final deletion callbacks
                 (let [cbs (o k)]
                   (doseq [cb cbs]
                     (cb)))))))

(defn register-delete-fn [key delete-fn]
  (swap! delete-fns
         update key
         (fn [v]
           (if v
             (conj v delete-fn)
             [delete-fn]))))

(defn del-reference [key]
  (references/delete-key key)
  (swap! delete-fns dissoc key) ;; removing the key triggers deletion callbacks
  nil)

(defn get-references []
  (-> @references/references
      :key->instance
      keys))
