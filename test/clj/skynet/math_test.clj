(ns skynet.math-test
  (:require
    [clojure.datafy :refer [datafy]]
    [clojure.test :refer [deftest is]]
    [skynet.math :as math])
  (:import
    (com.springrts.ai.oo AIFloat3)))


(set! *warn-on-reflection* true)


(def coldsnap-v2-metal
  [[120.0, 1184.5963, 4248.0]
   [56.0, 1147.2861, 360.0]
   [72.0, 1184.5963, 136.0]
   [4984.0, 1184.5963, 680.0]
   [4216.0, 1184.5963, 136.0]
   [3976.0, 1184.5963, 3320.0]
   [3512.0, 1184.5963, 3640.0]
   [1592.0, 2378.52, 2536.0]
   [2088.0, 2378.52, 1272.0]
   [2328.0, 2378.52, 3192.0]
   [1736.0, 2378.52, 1672.0]
   [744.0, 1184.5963, 5000.0]
   [1192.0, 1184.5963, 1416.0]
   [1576.0, 1184.5963, 1096.0]
   [2872.0, 2378.52, 1256.0]
   [3064.0, 2378.52, 3272.0]
   [3240.0, 2378.52, 1400.0]
   [3752.0, 2378.52, 2328.0]
   [3576.0, 2378.52, 1832.0]
   [3544.0, 2378.52, 3032.0]
   [4328.0, 1781.5582, 2072.0]
   [4120.0, 1781.5582, 2280.0]
   [3112.0, 1781.5582, 968.0]
   [3384.0, 1781.5582, 840.0]
   [952.0, 1781.5582, 2552.0]
   [664.0, 1781.5582, 2664.0]
   [2200.0, 1781.5582, 3944.0]
   [2408.0, 1781.5582, 3736.0]
   [5048.0, 1054.0107, 4776.0]
   [4856.0, 1054.0107, 5048.0]
   [5048.0, 1026.0283, 5032.0]
   [264.0, 1184.5963, 120.0]])


(deftest cluster-metal
  (let [clustered (math/cluster
                    (map
                      (fn [[x y z]] (AIFloat3. x y z))
                      coldsnap-v2-metal)
                    1300)]
    (is (= 32
           (count coldsnap-v2-metal)))
    (is (= 8
           (count clustered)))
    (is (= [[[2872 2378 1256]
             [3240 2378 1400]
             [3752 2378 2328]
             [3576 2378 1832]
             [3544 2378 3032]
             [4328 1781 2072]
             [4120 1781 2280]
             [3112 1781 968]
             [3384 1781 840]]
            [[1592 2378 2536]
             [2328 2378 3192]
             [1736 2378 1672]
             [952 1781 2552]
             [664 1781 2664]]
            [[3512 1184 3640]
             [3064 2378 3272]
             [2200 1781 3944]
             [2408 1781 3736]]
            [[56 1147 360] [72 1184 136] [264 1184 120]]
            [[5048 1054 4776] [4856 1054 5048] [5048 1026 5032]]
            [[120 1184 4248] [744 1184 5000]]
            [[4984 1184 680] [4216 1184 136]]
            [[1192 1184 1416] [1576 1184 1096]]]
           (->> clustered
                (map (partial map datafy))
                (map (partial map (partial map int))))))
    (is (= [[3547 2113 1778]
            [1454 2139 2523]
            [2796 1781 3648]
            [130 1172 205]
            [4984 1044 4952]
            [432 1184 4624]
            [4600 1184 408]
            [1384 1184 1256]]
           (map (comp (partial map int) datafy math/average) clustered)))))
