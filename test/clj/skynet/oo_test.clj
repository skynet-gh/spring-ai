(ns skynet.oo-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [skynet.oo :as oo])
  (:import
    (com.springrts.ai.oo.clb Map OOAICallback Resource)))


(deftest discover-resources
  (let [map-obj (reify Map
                  (getResourceMapSpotsPositions [this resource]
                    (case (.getName resource)
                      "Metal" []
                      "Energy" []))
                  (getMinWind [this]
                    1.0)
                  (getMaxWind [this]
                    10.0))
        callback (reify OOAICallback
                   (getResources [this]
                     [(reify Resource
                        (getName [this]
                          "Metal"))
                      (reify Resource
                        (getName [this]
                          "Energy"))])
                   (getMap [this]
                     map-obj))
        state (atom {})]
    (oo/discover-resources state callback)
    (is (= {:metal-spots nil
            :energy-spots nil
            :min-wind 1.0
            :avg-wind 5.5
            :max-wind 10.0}
           (dissoc
             @state
             :resources)))))
