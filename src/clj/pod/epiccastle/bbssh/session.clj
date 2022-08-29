(ns pod.epiccastle.bbssh.session
  (:require [pod.epiccastle.bbssh.impl.session :as session]
            [pod.epiccastle.bbssh.cleaner :as cleaner]))

(defn new [agent username host port]
  (cleaner/register
   (session/new agent username host port)))
