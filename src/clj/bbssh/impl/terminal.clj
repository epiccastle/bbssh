(ns bbssh.impl.terminal)

(set! *warn-on-reflection* true)

(def ctrl-c 3)
(def carriage-return 10)

(defn flush-out
  "flush standard out stream"
  []
  (.flush *out*))

(defn raw-mode-read-line
  "Read from stdin with terminal in raw mode. Detect ctrl-c break
  input and exit if found. Ends input on detection of carridge return.

  returns: the entered string or nil if ctrl-c pressed
  "
  []
  (BbsshUtils/enter-raw-mode 0)
  (let [result
        (loop [text ""]
          (let [c (.read ^clojure.lang.LineNumberingPushbackReader *in*)]
            (condp = c
              ctrl-c nil
              carriage-return text
              (recur (str text (char c))))))]
    (BbsshUtils/leave-raw-mode 0)
    result))

(defn print-flush-ask-yes-no
  "prompt the user at the terminal with a yes/no question string `s`.
  return true if they respond yes, or nil if not"
  [s]
  (print (str s " "))
  (flush-out)
  (some-> (read-line)
          first
          #{\y \Y}
          boolean))

(defn print-flush-ask-no-echo
  "Prompt the user at the terminal with a prompt string `s`.
  Disable terminal echo while they type. Return what they typed."
  [s]
  (print (str s " "))
  (flush-out)
  (raw-mode-read-line))
