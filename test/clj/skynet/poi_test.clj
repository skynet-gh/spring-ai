(ns skynet.poi-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [skynet.math :as math]
    [skynet.poi :as poi]))


(deftest next-building
  (is (= :skynet.unit/mex
         (poi/next-building
           {:poi-dist 0
            :positions (map math/float3 [[0 1 2] [3 4 5] [6 7 8]])
            :units ["armmex"
                    "armmex"]})))
  (is (= :skynet.unit/solar
         (poi/next-building
           {:poi-dist 0
            :positions (map math/float3 [[0 1 2] [3 4 5] [6 7 8]])
            :units ["armmex"
                    "armmex"
                    "armmex"]})))
  (is (= :skynet.unit/lab
         (poi/next-building
           {:poi-dist 0
            :positions (map math/float3 [[0 1 2] [3 4 5] [6 7 8]])
            :units ["armmex"
                    "armmex"
                    "armmex"
                    "armsolar"]})))
  (is (= :skynet.unit/solar
         (poi/next-building
           {:poi-dist 0
            :positions (map math/float3 [[0 1 2] [3 4 5] [6 7 8]])
            :units ["armmex"
                    "armmex"
                    "armmex"
                    "armsolar"
                    "armlab"]})))
  (is (= :skynet.unit/radar
         (poi/next-building
           {:poi-dist 1
            :positions (map math/float3 [[0 1 2] [3 4 5] [6 7 8]])
            :units ["armmex"
                    "armmex"
                    "armmex"
                    "armllt"]})))
  (is (= :skynet.unit/llt
         (poi/next-building
           {:poi-dist 1
            :positions (map math/float3 [[0 1 2] [3 4 5] [6 7 8]])
            :units ["armmex"
                    "armmex"
                    "armmex"
                    "armllt"
                    "armrad"]})))
  (is (= :skynet.unit/hlt
         (poi/next-building
           {:poi-dist 1
            :positions (map math/float3 [[0 1 2] [3 4 5] [6 7 8]])
            :units ["armmex"
                    "armmex"
                    "armmex"
                    "armllt"
                    "armrad"
                    "armllt"
                    "armllt"]})))
  (is (= :skynet.unit/coastal
         (poi/next-building
           {:poi-dist 1
            :positions (map math/float3 [[0 1 2] [3 4 5]])
            :units ["armmex"
                    "armmex"
                    "armllt"
                    "armrad"
                    "armllt"
                    "armllt"
                    "armhlt"
                    "armllt"]})))
  (is (= :skynet.unit/llt
         (poi/next-building
           {:poi-dist 1
            :positions (map math/float3 [[0 1 2] [3 4 5]])
            :units ["armmex"
                    "armmex"
                    "armllt"
                    "armrad"
                    "armllt"
                    "armllt"
                    "armhlt"
                    "armllt"
                    "armguard"]}))))
