(ns skynet.util
  (:require
    [clojure.core.protocols]
    [clojure.datafy :refer [datafy]]
    [clojure.pprint :refer [pprint]]
    [taoensso.timbre :as log])
  (:import
    (com.springrts.ai.oo AIFloat3)))

(extend-protocol clojure.core.protocols/Datafiable
  AIFloat3
  (datafy [pos]
    [(.x pos) (.y pos) (.z pos)]))


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

(defn normalize-resource 
  "Returns the resource position with the y value zeroed, since it represents resource value."
  [res-pos]
  (AIFloat3. (.x res-pos) 0 (.z res-pos))) ; y is resource


(defn distance
  "Calculates the distance between two (AIFloat3) points."
  [a b]
  (let [xd (- (.x a) (.x b))
        yd (- (.y a) (.y b))
        zd (- (.z a) (.z b))]
    (Math/sqrt
      (+ (* xd xd) (* yd yd) (* zd zd)))))
