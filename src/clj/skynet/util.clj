(ns skynet.util
  (:require
    [clojure.pprint :refer [pprint]]
    [taoensso.timbre :as log]))


(defn pprint-str [d]
  (with-out-str
    (pprint d)))

(defmacro try-log
  "Try to execute body, catch Throwable, and log it."
  [fn-name & body]
  `(try
     ~@body
     (catch Throwable e#
       (log/error e# (str "Error in " ~fn-name))
       -1)))

(defn distance
  "Calculates the distance between two (AIFloat3) points."
  [a b]
  (let [xd (- (.x a) (.x b))
        yd (- (.y a) (.y b))
        zd (- (.z a) (.z b))]
    (+ (* xd xd) (* yd yd) (* zd zd))))
