(ns skynet.strategy
  (:require
    [clustering.core.qt :as qt]
    [skynet.strategy.base :as base]
    [skynet.util :as u]))


(defn build-next [phases])



(defmulti goal (fn [state unit] (.getName (.getDef unit))))


(defmethod goal "armcom" [state unit]
  ())
