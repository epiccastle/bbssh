(ns bbssh.impl.lookup
  (:require [pod.epiccastle.bbssh.impl.agent]
            [pod.epiccastle.bbssh.impl.cleaner]
            [pod.epiccastle.bbssh.impl.session]))

(defmacro ns-lookups [namespaces]
  (into {}
        (for [namespace namespaces
              [name var] (ns-publics namespace)]
          [(list 'quote (symbol (str namespace) (str name)))
           (symbol var)])))

#_ (macroexpand-1 '(ns-lookups [pod.epiccastle.bbssh.impl.session
                                pod.epiccastle.bbssh.impl.agent
                                ]))

(def lookup
  (ns-lookups [pod.epiccastle.bbssh.impl.agent
               pod.epiccastle.bbssh.impl.session
               pod.epiccastle.bbssh.impl.cleaner])
  #_{
   'pod.epiccastle.bbssh.impl.agent/new
   pod.epiccastle.bbssh.impl.agent/new

   'pod.epiccastle.bbssh.impl.agent/get-session
   pod.epiccastle.bbssh.impl.agent/get-session

   'pod.epiccastle.bbssh.impl.session/new
   pod.epiccastle.bbssh.impl.session/new

   'pod.epiccastle.bbssh.impl.cleaner/del-reference
   pod.epiccastle.bbssh.impl.cleaner/del-reference

   'pod.epiccastle.bbssh.impl.cleaner/get-references
   pod.epiccastle.bbssh.impl.cleaner/get-references

   })
