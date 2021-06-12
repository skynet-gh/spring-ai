(ns skynet.unit-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [skynet.unit :as unit])
  (:import
    (com.springrts.ai.oo.clb Unit UnitDef)))


(deftest typeof
  (is (= ::unit/com
         (unit/typeof "armcom")))
  (is (= ::unit/com
         (unit/typeof (reify UnitDef
                        (getName [this]
                          "armcom")))))
  (is (= ::unit/com
         (unit/typeof
           (reify Unit
             (getDef [this]
               (reify UnitDef
                 (getName [this]
                   "armcom"))))))))
