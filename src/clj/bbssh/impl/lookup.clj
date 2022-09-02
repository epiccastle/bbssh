(ns bbssh.impl.lookup
  (:require [pod.epiccastle.bbssh.impl.agent]
            [pod.epiccastle.bbssh.impl.cleaner]
            [pod.epiccastle.bbssh.impl.session]
            [pod.epiccastle.bbssh.impl.channel-exec]
            [pod.epiccastle.bbssh.impl.input-stream]
            [pod.epiccastle.bbssh.impl.output-stream]
            [pod.epiccastle.bbssh.impl.user-info]
            [pod.epiccastle.bbssh.impl.callbacks]))

(defmacro ns-lookups [namespaces]
  (into {}
        (for [namespace namespaces
              [name var] (ns-publics namespace)]
          [(list 'quote (symbol (str namespace) (str name)))
            var])))

#_ (macroexpand-1 '(ns-lookups [pod.epiccastle.bbssh.impl.session
                                pod.epiccastle.bbssh.impl.agent
                                ]))

(def lookup
  (ns-lookups [pod.epiccastle.bbssh.impl.agent
               pod.epiccastle.bbssh.impl.session
               pod.epiccastle.bbssh.impl.cleaner
               pod.epiccastle.bbssh.impl.channel-exec
               pod.epiccastle.bbssh.impl.input-stream
               pod.epiccastle.bbssh.impl.output-stream
               pod.epiccastle.bbssh.impl.user-info
               pod.epiccastle.bbssh.impl.callbacks
               ])
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
