(ns bbssh.impl.references
  (:require [clojure.string :as string]))

(set! *warn-on-reflection* true)

;; All the pod java instances need to stay in the pod heap memory.
;; On the babashka side, we need to refer to these objects with keywords.
;; We need to look up an instance from a keyword, and a keyword from
;; an instance.

(def key-set (into [] "0123456789abcdefghijklmnopqrstuvwxyz"))
(def key-length 16)

;; on the pod side, keys are a vector of [namespace name] where
;; both are strings. but on the bb side, these are used to construct
;; (transient) keywords that will be eventually garbage collected.
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

(defn add-instance*
  "returns the new references state with the instance added"
  [state instance key-ns key-prefix]
  (let [{:keys [instance->key]} state]
    (if (get instance->key instance)
      state
      (let [new-key (make-key key-ns key-prefix)]
        (-> state
            (assoc-in [:instance->key instance] new-key)
            (assoc-in [:key->instance new-key] instance))))))

(defn add-instance
  [instance & [{:keys [key-ns key-prefix]}]]
  (let [the-class ^java.lang.Class (class instance)
        simple-name (.getSimpleName the-class)
        package-name (.getPackageName the-class)
        ]
    (-> (swap! references add-instance* instance
               (or key-ns package-name)
               (or key-prefix simple-name))
        (get-in [:instance->key instance]))))

(defn get-key* [state instance]
  (get-in state [:instance->key instance]))

(defn get-key [instance]
  (get-key* @references instance))

(defn get-instance* [state key]
  (get-in state [:key->instance key]))

(defn get-instance [key]
  (get-instance* @references key))

(defn delete-instance* [state instance]
  (let [key (get-key* state instance)]
    (-> state
        (update :instance->key dissoc instance)
        (update :key->instance dissoc key))))

(defn delete-instance [instance]
  (swap! references delete-instance* instance))

(defn delete-key* [state key]
  (let [instance (get-instance* state key)]
    (-> state
        (update :instance->key dissoc instance)
        (update :key->instance dissoc key))))

(defn delete-key [key]
  (swap! references delete-key* key))
