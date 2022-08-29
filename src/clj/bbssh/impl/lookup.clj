(ns bbssh.impl.lookup
  (:require [pod.epiccastle.bbssh.impl.core]))

(def lookup
  {
   'pod.epiccastle.bbssh.impl.core/new-agent
   pod.epiccastle.bbssh.impl.core/new-agent

   'pod.epiccastle.bbssh.impl.core/del-agent
   pod.epiccastle.bbssh.impl.core/del-agent

   'pod.epiccastle.bbssh.impl.core/raise
   pod.epiccastle.bbssh.impl.core/raise

   })
