(ns bbssh.impl.lookup
  (:require [pod.epiccastle.bbssh.impl.agent]
            [pod.epiccastle.bbssh.impl.cleaner]
            [pod.epiccastle.bbssh.impl.session]))

(def lookup
  {
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
