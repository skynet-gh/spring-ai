(ns skynet.strategy.base
  (:require
    [skynet.unit :as u]))


(def phases
  {:base
   [{:name "start"
     :buildings
     [[::u/solar 2]
      [::u/kbot-lab 1]
      [::u/radar 1]
      [::u/llt 1]]}]
   :forward
   [{:name "defense"
     :buildings
     [[::u/llt 2]
      [::u/hlt 1]
      [::u/llt 1]
      [::u/coastal 1]
      [::u/llt 1]
      [::u/hlt 1]
      [::u/llt 1]
      [::u/coastal 1]]}]})


(defn poi-phases
  [poi]
  (get phases
    (case (:poi-dist poi)
      0 :base
      :forward)))


(defn next-building
  "Returns the keyword for the unit type that should be built next at this POI."
  [{:keys [positions units] :as poi}]
  (let [mexes (filter (comp #{::u/mex} u/typeof) units)]
    (if (< (count mexes) (count positions))
      ::u/mex
      (let [phases (poi-phases poi)
            phase (first phases)] ; TODO
        (reduce
          (fn [remaining [building n]]
            (if (keyword? remaining)
              remaining
              (let [matching (filter (comp #{building} u/typeof second) remaining)]
                (if (< (count matching) n)
                  building
                  (apply dissoc remaining (map first (take n matching)))))))
          (into {}
            (map-indexed vector (remove (comp #{::u/mex} u/typeof) units)))
          (:buildings phase))))))
