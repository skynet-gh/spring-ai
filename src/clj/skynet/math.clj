(ns skynet.math
  (:require
    [clojure.core.matrix :as matrix]
    [clojure.datafy :refer [datafy]]
    [clustering.core.qt :as qt])
  (:import
    (com.springrts.ai.oo AIFloat3)))


(set! *warn-on-reflection* true)


(def default-cluster-distance 800) ;900 ;1000)


(defn float3
  "Returns the given vector position as an AIFloat3 (inverse of datafy)."
  [[x y z]]
  (AIFloat3. x y z))


(def nil-point
  (float3 [-1 0 0]))


(defn average [points]
  (let [data (map datafy points)]
    (float3
      (matrix/scale
        (apply matrix/add data)
        (/ 1.0 (count points))))))


(defn distance
  "Calculates the distance between two (AIFloat3) points."
  [a b]
  (let [xd (- (.x a) (.x b))
        yd (- (.y a) (.y b))
        zd (- (.z a) (.z b))]
    (Math/sqrt
      (+ (* xd xd) (* yd yd) (* zd zd)))))


(defn cluster
  ([positions]
   (cluster positions default-cluster-distance))
  ([positions cluster-distance]
   (qt/cluster distance positions cluster-distance 2)))


(defn normalize-resource
  "Returns the resource position with the y value zeroed, since it represents resource value."
  [res-pos]
  (AIFloat3. (.x res-pos) 0 (.z res-pos))) ; y is resource
