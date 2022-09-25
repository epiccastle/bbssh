(ns bbssh.impl.lookup
  (:require [pod.epiccastle.bbssh.pod.agent]
            [pod.epiccastle.bbssh.pod.terminal]
            [pod.epiccastle.bbssh.pod.cleaner]
            [pod.epiccastle.bbssh.pod.session]
            [pod.epiccastle.bbssh.pod.channel-exec]
            [pod.epiccastle.bbssh.pod.input-stream]
            [pod.epiccastle.bbssh.pod.output-stream]
            [pod.epiccastle.bbssh.pod.byte-array-input-stream]
            [pod.epiccastle.bbssh.pod.byte-array-output-stream]
            [pod.epiccastle.bbssh.pod.user-info]
            [pod.epiccastle.bbssh.pod.identity]
            [pod.epiccastle.bbssh.pod.identity-repository]
            [pod.epiccastle.bbssh.pod.callbacks]
            [pod.epiccastle.bbssh.pod.key-pair]
            [pod.epiccastle.bbssh.pod.host-key]
            [pod.epiccastle.bbssh.pod.known-hosts]
            [pod.epiccastle.bbssh.pod.host-key-repository]))

(defmacro ns-lookups [namespaces]
  (into {}
        (for [namespace namespaces
              [name var] (ns-publics namespace)]
          [(list 'quote (symbol (str namespace) (str name)))
            var])))

#_ (macroexpand-1 '(ns-lookups [pod.epiccastle.bbssh.pod.session
                                pod.epiccastle.bbssh.pod.agent
                                ]))

(def lookup
  (ns-lookups [pod.epiccastle.bbssh.pod.agent
               pod.epiccastle.bbssh.pod.session
               pod.epiccastle.bbssh.pod.terminal
               pod.epiccastle.bbssh.pod.cleaner
               pod.epiccastle.bbssh.pod.channel-exec
               pod.epiccastle.bbssh.pod.input-stream
               pod.epiccastle.bbssh.pod.output-stream
               pod.epiccastle.bbssh.pod.byte-array-input-stream
               pod.epiccastle.bbssh.pod.byte-array-output-stream
               pod.epiccastle.bbssh.pod.user-info
               pod.epiccastle.bbssh.pod.identity
               pod.epiccastle.bbssh.pod.identity-repository
               pod.epiccastle.bbssh.pod.callbacks
               pod.epiccastle.bbssh.pod.key-pair
               pod.epiccastle.bbssh.pod.host-key
               pod.epiccastle.bbssh.pod.known-hosts
               pod.epiccastle.bbssh.pod.host-key-repository
               ]))
