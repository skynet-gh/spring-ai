(ns skynet.math
  (:require
    [clojure.core.matrix :as matrix]
    [clojure.datafy :refer [datafy]]
    [clustering.core.qt :as qt]
    [skynet.util :as u])
  (:import
    (com.springrts.ai.oo AIFloat3)))



(def default-cluster-distance 2000)


(defn float3
  "Returns the given vector position as an AIFloat3 (inverse of datafy)."
  [[x y z]]
  (AIFloat3. x y z))


(defn average [points]
  (let [data (map datafy points)]
    (float3
      (matrix/scale
        (apply matrix/add data)
        (/ 1.0 (count points))))))


(defn cluster
  ([positions]
   (cluster positions default-cluster-distance))
  ([positions cluster-distance]
   (qt/cluster u/distance positions cluster-distance 2)))
