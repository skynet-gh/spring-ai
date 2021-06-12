(ns skynet
  (:require
    [chime.core :as chime]
    [clojure.core.protocols]
    [clojure.string :as string]
    [java-time :as jt]
    [skynet.math :as math]
    [skynet.old :as old]
    [skynet.strategy.base :as base]
    [skynet.unit :as unit]
    [skynet.util :as u]
    [taoensso.timbre :as log])
  (:import
    (com.springrts.ai.oo AIFloat3)
    (com.springrts.ai.oo.clb Game Map Point OOAICallback Unit)))


(set! *warn-on-reflection* true)


(def default-timeout 10000)


(defn unit-data [resources ^Unit unit]
  (let [unit-def (.getDef unit)]
    {:name (.getName unit-def)
     :pos (.getPos unit)
     :build-speed (.getBuildSpeed unit-def)
     :use-metal (.getResourceUse unit (:metal resources))
     :use-energy (.getResourceUse unit (:energy resources))}))


(defn handle-error [exception]
  (log/error exception "An error occurred in async")
  true)


(def ignore-idle
  #{"armmex" "armsolar" "armwin" "armmakr" "armadvsol" "armmoho" "armmmkr" "armfus"})


(defn filter-idle [units]
  (->> units
       (remove (comp ignore-idle #(.getName %) #(.getDef %)))
       (filter (comp empty? #(.getCurrentCommands %)))))


(defn world
  "Returns data about the world obtained from state."
  [{:keys [^OOAICallback callback resources metal-positions metal-clusters] :as state}]
  (let [map-obj (.getMap callback)
        team-units (.getTeamUnits callback)
        com-name (first
                   (filter (comp #{"armcom" "corcom"})
                           (map (comp #(.getName %) #(.getDef %)) team-units)))
        builder-units (filter (comp #(.isBuilder %) #(.getDef %)) team-units)
        mexes (filter (comp pos? #(.getExtractsResource % (:metal resources)) #(.getDef %)) team-units)
        mex-spots (->> mexes
                       (map
                         (fn [mex]
                           {(.getResourceMapSpotsNearest map-obj (:metal resources) (.getPos mex))
                            {(.getName (.getDef mex)) mex}}))
                       (merge-with merge))
        metal-details (into {}
                        (map
                          (fn [pos]
                            [pos {::mexes (get mex-spots pos)}])
                          metal-positions))
        start-pos (.getStartPos map-obj)
        pois (->> metal-clusters
                  (sort-by (comp (partial u/distance start-pos) :center))
                  (map-indexed
                    (fn [n cluster]
                      (let [^AIFloat3 center (:center cluster)]
                        (assoc cluster
                          :poi-dist n
                          :units (.getFriendlyUnitsIn callback center math/default-cluster-distance))))))
        filter-side (case com-name
                      "armcom" (fn [d] (string/starts-with? (.getName d) "arm"))
                      "corcom" (fn [d] (string/starts-with? (.getName d) "cor"))
                      identity)
        unit-defs (filter filter-side (.getUnitDefs callback))
        unit-defs-by-name (into {}
                            (map (juxt #(.getName %) identity)
                                 unit-defs))
        unit-defs-by-type (->> unit-defs
                               (map (juxt (comp unit/typeof #(.getName %)) identity))
                               (into {}))
        unit-def-names-by-type (->> unit-defs
                                    (map (juxt (comp unit/typeof #(.getName %)) #(.getName %)))
                                    (into {}))]
    {::economy (old/economy state)
     ::pois pois
     ::idle-units (filter-idle team-units)
     ::builders builder-units
     ::builders-data (map (partial unit-data resources) builder-units)
     ::team-units team-units
     ::metal-details metal-details
     ::unit-defs unit-defs
     ::unit-defs-by-name unit-defs-by-name
     ::unit-defs-by-type unit-defs-by-type
     ::unit-def-names-by-type unit-def-names-by-type}))


(defn give-commands
  [{:keys [^OOAICallback callback]} {::keys [pois unit-defs-by-type metal-details]}]
  (let [^Map map-obj (.getMap callback)
        points (.getPoints map-obj false)
        _point-poses (set (map (fn [^Point point] (.getPosition point)) points))
        ^Game game (.getGame callback)]
    #_ ; does nothing
    (doseq [poi pois]
      (let [center (:center poi)
            zone (rand-int 256)] ; why
        (.sendTextMessage game (str (:poi-dist poi) " z" zone) zone)
        (.setLastMessagePosition game center)))
    (doseq [poi pois]
      (log/debug "POI" (:poi-dist poi) "at" (:center poi))
      (let [next-build-type (base/next-building poi)
            idle-units (filter-idle (:units poi))
            build-def (get unit-defs-by-type next-build-type)
            build-pos (cond
                        (= ::unit/mex next-build-type)
                        (->> poi
                             :positions 
                             (remove (comp ::mexes metal-details))
                             (filter #(.isPossibleToBuildAt map-obj build-def % 0))
                             first)
                        build-def
                        (.findClosestBuildSite map-obj build-def (:center poi) math/default-cluster-distance 0 0)
                        :else 
                        nil)]
        (doseq [^Unit unit (filter (comp #(.isBuilder %) #(.getDef %)) idle-units)]
          (if build-def
            (do
              (log/debug "Unit" unit "which is an" (unit/def-name unit) "which is type" (unit/typeof unit)
                         "should build" next-build-type)
              (.build unit build-def build-pos 0 (short 0) default-timeout))
            (when-let [next-poi (first (filter #(< (:poi-dist poi) (:poi-dist %)) pois))]
              (log/debug "Unit" unit "moving to POI" (:poi-dist next-poi))
              (.moveTo unit (:center next-poi) (short 0) default-timeout))))))))


(defn run [state-atom]
  (log/info "Checking world state")
  (let [state @state-atom
        world-data (world state)]
    (give-commands state world-data)
    #_
    (dorun
      (map
        (fn [^Unit unit]
          (log/info "Giving" unit "type" (.getName (.getDef unit)) "a job")
          (old/assign-unit state unit))
        (filter-idle
          (.getTeamUnits ^OOAICallback (:callback state)))))))


(defn init [state]
  (log/info "Init async logic")
  (let [now (jt/instant)]
    (chime/chime-at
      (chime/periodic-seq now (jt/duration 1000 :millis))
      (fn [_chimestamp]
        (run state))
      {:error-handler handle-error})))
