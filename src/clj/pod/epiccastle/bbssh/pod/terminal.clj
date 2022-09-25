(ns pod.epiccastle.bbssh.impl.terminal)

(defn is-terminal? []
  (pos? (BbsshUtils/is-a-tty)))

(defn get-width []
  (BbsshUtils/get-terminal-width))

(defn enter-raw-mode [n]
  (BbsshUtils/enter-raw-mode n))

(defn leave-raw-mode [n]
  (BbsshUtils/leave-raw-mode n))
