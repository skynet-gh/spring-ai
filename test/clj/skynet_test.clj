(ns skynet-test
  (:require
    [clojure.string :as string]
    [clojure.test :refer [deftest is testing]]
    [skynet])
  (:import
    (com.springrts.ai.oo AIFloat3)
    (com.springrts.ai.oo.clb Economy Map OOAICallback Resource)))


(deftest init
  (let [chimer (skynet/init nil)]
    (try
      (is (instance? java.lang.AutoCloseable chimer))
      (finally
        (.close chimer)))))


(def initial-economy
  {:metal
   {:received 0.0,
    :excess 0.0,
    :sent 0.0,
    :usage 0.0,
    :share 0.99,
    :current 1000.0,
    :storage 1000.0,
    :pull 0.0,
    :income 0.0},
   :energy
   {:received 0.0,
    :excess 0.0,
    :sent 0.0,
    :usage 0.0,
    :share 0.95,
    :current 1000.0,
    :storage 1000.0,
    :pull 0.0,
    :income 0.0}})

(def later-economy
  {:metal
   {:received 0.0,
    :excess 0.0,
    :sent 0.0,
    :usage 6.842015,
    :share 0.99,
    :current 85.62922,
    :storage 2250.0,
    :pull 6.842015,
    :income 5.708743},
   :energy
   {:received 0.0,
    :excess 0.0,
    :sent 0.0,
    :usage 97.722534,
    :share 0.95,
    :current 260.60312,
    :storage 1250.5,
    :pull 97.722534,
    :income 92.74297}})

(defn get-initial [resource metric]
  (when resource
    (get-in initial-economy [(keyword (string/lower-case (.getName resource))) metric])))


(deftest world
  (testing "initial"
    (let [map-obj (reify Map
                    (getStartPos [this]
                      (AIFloat3. 1 1 1))
                    (getResourceMapSpotsNearest [this resource pos]
                      (case (.getName resource)
                        "Metal" (AIFloat3. 0 0 0)
                        "Energy" (AIFloat3. 0 0 0))))
          economy (reify Economy
                    (getCurrent [this resource]
                      (get-initial resource :current))
                    (getIncome [this resource]
                      (get-initial resource :income))
                    (getUsage [this resource]
                      (get-initial resource :usage))
                    (getStorage [this resource]
                      (get-initial resource :storage))
                    (getPull [this resource]
                      (get-initial resource :pull))
                    (getShare [this resource]
                      (get-initial resource :share))
                    (getSent [this resource]
                      (get-initial resource :sent))
                    (getReceived [this resource]
                      (get-initial resource :received))
                    (getExcess [this resource]
                      (get-initial resource :excess)))
          callback (reify OOAICallback
                     (getMap [this]
                       map-obj)
                     (getTeamUnits [this]
                       [])
                     (getEconomy [this]
                       economy)
                     (getUnitDefs [this]
                       []))
          state {:callback callback
                 :resources {:metal (reify Resource
                                      (getName [this]
                                        "Metal"))
                             :energy (reify Resource
                                       (getName [this]
                                         "Energy"))}}]
      (is (= {:skynet/pois (),
              :skynet/team-units [],
              :skynet/builders-data (),
              :skynet/metal-details {},
              :skynet/unit-defs-by-name {},
              :skynet/unit-defs (),
              :skynet/unit-defs-by-type {},
              :skynet/builders (),
              :skynet/idle-units (),
              :skynet/unit-def-names-by-type {}}
             (dissoc
               (skynet/world state)
               :skynet/economy))))))
