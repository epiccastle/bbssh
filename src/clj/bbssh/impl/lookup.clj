(ns bbssh.impl.lookup
  (:require [pod.epiccastle.bbssh.impl.agent]
            [pod.epiccastle.bbssh.impl.terminal]
            [pod.epiccastle.bbssh.impl.cleaner]
            [pod.epiccastle.bbssh.impl.session]
            [pod.epiccastle.bbssh.impl.channel-exec]
            [pod.epiccastle.bbssh.impl.input-stream]
            [pod.epiccastle.bbssh.impl.output-stream]
            [pod.epiccastle.bbssh.impl.byte-array-input-stream]
            [pod.epiccastle.bbssh.impl.byte-array-output-stream]
            [pod.epiccastle.bbssh.impl.user-info]
            [pod.epiccastle.bbssh.impl.identity]
            [pod.epiccastle.bbssh.impl.identity-repository]
            [pod.epiccastle.bbssh.impl.callbacks]
            [pod.epiccastle.bbssh.impl.key-pair]
            [pod.epiccastle.bbssh.impl.host-key]
            [pod.epiccastle.bbssh.impl.utils]
            [pod.epiccastle.bbssh.impl.known-hosts]
            [pod.epiccastle.bbssh.impl.host-key-repository]))

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
               pod.epiccastle.bbssh.impl.terminal
               pod.epiccastle.bbssh.impl.cleaner
               pod.epiccastle.bbssh.impl.channel-exec
               pod.epiccastle.bbssh.impl.input-stream
               pod.epiccastle.bbssh.impl.output-stream
               pod.epiccastle.bbssh.impl.byte-array-input-stream
               pod.epiccastle.bbssh.impl.byte-array-output-stream
               pod.epiccastle.bbssh.impl.user-info
               pod.epiccastle.bbssh.impl.identity
               pod.epiccastle.bbssh.impl.identity-repository
               pod.epiccastle.bbssh.impl.callbacks
               pod.epiccastle.bbssh.impl.key-pair
               pod.epiccastle.bbssh.impl.host-key
               pod.epiccastle.bbssh.impl.utils
               pod.epiccastle.bbssh.impl.known-hosts
               pod.epiccastle.bbssh.impl.host-key-repository
               ]))
