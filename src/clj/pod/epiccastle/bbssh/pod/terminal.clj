(ns pod.epiccastle.bbssh.pod.terminal)

(defn is-terminal? []
  (pos? (BbsshUtils/is-stdout-a-tty))
  )

(defn get-width []
  (BbsshUtils/get-terminal-width)
  )

(defn get-height []
  (BbsshUtils/get-terminal-height)
  )

(defn enter-raw-mode [n]
  (BbsshUtils/enter-raw-mode n)
  )

(defn leave-raw-mode [n]
  (BbsshUtils/leave-raw-mode n)
  )
