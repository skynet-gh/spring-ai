(ns skynet.strategy.base-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [skynet.math :as math]
    [skynet.strategy.base :as base])
  (:import
    (com.springrts.ai.oo.clb Unit UnitDef)))


(defn mock-unit [def-name]
  (reify Unit
    (getDef [this]
      (reify UnitDef
        (getName [this]
          def-name)))))


(deftest next-building
  (is (= :skynet.unit/mex
         (base/next-building
           {:poi-dist 0
            :positions (map math/float3 [[0 1 2] [3 4 5] [6 7 8]])
            :units ["armmex"
                    "armmex"]})))
  (is (= :skynet.unit/solar
         (base/next-building
           {:poi-dist 0
            :positions (map math/float3 [[0 1 2] [3 4 5] [6 7 8]])
            :units ["armmex"
                    "armmex"
                    "armmex"]})))
  (is (= :skynet.unit/solar
         (base/next-building
           {:poi-dist 0
            :positions (map math/float3 [[0 1 2] [3 4 5] [6 7 8]])
            :units ["armmex"
                    "armmex"
                    "armmex"
                    "armsolar"]})))
  (is (= :skynet.unit/kbot-lab
         (base/next-building
           {:poi-dist 0
            :positions (map math/float3 [[0 1 2] [3 4 5] [6 7 8]])
            :units ["armmex"
                    "armmex"
                    "armmex"
                    "armsolar"
                    "armsolar"]})))
  (is (= :skynet.unit/llt
         (base/next-building
           {:poi-dist 1
            :positions (map math/float3 [[0 1 2] [3 4 5] [6 7 8]])
            :units ["armmex"
                    "armmex"
                    "armmex"
                    "armllt"]})))
  (is (= :skynet.unit/hlt
         (base/next-building
           {:poi-dist 1
            :positions (map math/float3 [[0 1 2] [3 4 5] [6 7 8]])
            :units ["armmex"
                    "armmex"
                    "armmex"
                    "armllt"
                    "armllt"]})))
  (is (= :skynet.unit/coastal
         (base/next-building
           {:poi-dist 1
            :positions (map math/float3 [[0 1 2] [3 4 5]])
            :units ["armmex"
                    "armmex"
                    "armllt"
                    "armllt"
                    "armhlt"
                    "armllt"]})))
  (is (= :skynet.unit/llt
         (base/next-building
           {:poi-dist 1
            :positions (map math/float3 [[0 1 2] [3 4 5]])
            :units ["armmex"
                    "armmex"
                    "armllt"
                    "armllt"
                    "armhlt"
                    "armllt"
                    "armguard"]}))))
