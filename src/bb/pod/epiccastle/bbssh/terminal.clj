(ns pod.epiccastle.bbssh.terminal
  (:require [pod.epiccastle.bbssh.pod.terminal :as terminal]))

(defn is-terminal?
  "Returns true is stdout is connected to a terminal"
  []
  (terminal/is-terminal?))

(defn get-width
  "return the width of the terminal"
  []
  (terminal/get-width))

(defn enter-raw-mode
  "switch the present terminal into raw mode"
  [& [n]]
  (terminal/enter-raw-mode (or n 0)))

(defn leave-raw-mode
  "switch the present terminal out of raw mode"
  [& [n]]
  (terminal/leave-raw-mode (or n 0)))

(def ctrl-c 3)
(def carriage-return 10)

(defn raw-mode-readline
  "Read input from stdin with terminal in raw mode."
  []
  (enter-raw-mode)
  (let [result (loop [text ""]
                 (let [c (.read *in*)]
                   (condp = c
                     ctrl-c nil
                     carriage-return text
                     (recur (str text (char c))))))]
    (leave-raw-mode)
    result))
