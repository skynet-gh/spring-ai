(ns skynet.util
  (:require
    [clojure.core.protocols]
    [clojure.datafy :refer [datafy]]
    [clojure.pprint :refer [pprint]]
    [taoensso.timbre :as log])
  (:import
    (com.springrts.ai.oo AIFloat3)))


(set! *warn-on-reflection* true)


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
