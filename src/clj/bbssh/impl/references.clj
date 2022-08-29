(ns bbssh.impl.references)

;; All the pod java instances need to stay in the pod heap memory.
;; On the babashka side, we need to refer to these objects with keywords.
;; We need to look up an instance from a keyword, and a keyword from
;; an instance.

(def key-set (into [] "0123456789abcdefghijklmnopqrstuvwxyz"))
(def key-length 16)

;; on the pod side, keys are a vector of [namespace name] where
;; both are strings
(defn make-key [namespace prefix]
  (->> key-length
       range
       (map (fn [_] (rand-nth key-set)))
       (apply str prefix "-")
       (conj [namespace])))

;; we hold strong references here to prevent garbage collection.
;; when the babashka garbage collector cleans up an object, it triggers
;; a pod invoke that removes the reference from the collection here.
;; Then at some future time the pod garbage collector will collect the
;; actual object.
(defonce references
  (atom
   {
    ;; map java object instances to keys
    :instance->key {}

    ;; maps the reference keys to the object
    :key->instance {}}))

(defn add-instance
  "returns the new references state with the instance added"
  [state instance key-ns key-prefix]
  (let [{:keys [instance->key]} state]
    (if (get instance->key instance)
      state
      (let [new-key (make-key key-ns key-prefix)]
        (-> state
            (assoc-in [:instance->key instance] new-key)
            (assoc-in [:key->instance new-key] instance))))))

(defn add-instance!
  [instance key-ns key-prefix]
  (-> (swap! references add-instance instance key-ns key-prefix)
      (get-in [:instance->key instance])))

(defn get-key-for-instance [state instance]
  (get-in state [:instance->key instance]))

(defn get-key-for-instance! [instance]
  (get-key-for-instance @references instance))

(defn get-instance-for-key [state key]
  (get-in state [:key->instance key]))

(defn get-instance-for-key! [key]
  (get-instance-for-key @references key))

(defn delete-instance [references instance]
  (let [key (get-key-for-instance references instance)]
    (-> references
        (update :instance->key dissoc instance)
        (update :key->instance dissoc key))))

(defn delete-instance! [instance]
  (swap! references delete-instance instance))

(defn delete-key [state key]
  (let [instance (get-instance-for-key state key)]
    (-> state
        (update :instance->key dissoc instance)
        (update :key->instance dissoc key))))

(defn delete-key! [key]
  (swap! references delete-key key))
