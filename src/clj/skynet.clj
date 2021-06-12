(ns skynet
  (:require
    [chime.core :as chime]
    [clojure.core.protocols]
    [clojure.string :as string]
    [java-time :as jt]
    [skynet.math :as math]
    [skynet.old :as old]
    [skynet.poi :as poi]
    [skynet.unit :as unit]
    [skynet.util :as u]
    [taoensso.timbre :as log])
  (:import
    (com.springrts.ai.oo AIFloat3)
    (com.springrts.ai.oo.clb Game Map Point OOAICallback Unit UnitDef)))


(set! *warn-on-reflection* true)


(def tick-millis 500)

(def default-timeout 100000)


(def min-build-dists
  {::unit/lab 40
   ::unit/llt 20
   ::unit/hlt 20
   ::unit/coastal 30
   ::unit/converter 5})


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

(def ignore-idle-types
  #{::unit/mex ::unit/solar ::unit/wind ::unit/converter ::unit/adv-solar ::unit/moho
    ::unit/adv-converter ::unit/fusion ::unit/adv-fusion
    ::unit/llt ::unit/hlt ::unit/radar ::unit/coastal})


(defn filter-idle [units]
  (->> units
       (remove (comp ignore-idle-types unit/typeof))
       (filter (comp empty? (fn [^Unit unit] (.getCurrentCommands unit))))))

(defn get-def [^Unit unit]
  (.getDef unit))
(def builder?
  (comp (fn [^UnitDef unit-def] (when unit-def (.isBuilder unit-def))) get-def))
(def fighter?
  (comp (fn [^UnitDef unit-def] (when unit-def (.isAbleToFight unit-def))) get-def))

(defn world
  "Returns data about the world obtained from state."
  [{:keys [^OOAICallback callback resources metal-positions metal-clusters] :as state}]
  (let [map-obj (.getMap callback)
        team-units (.getTeamUnits callback)
        com-name (first
                   (filter (comp #{"armcom" "corcom"})
                           (map unit/def-name team-units)))
        builder-units (filter builder? team-units)
        get-extracts-metal (fn [^UnitDef unit-def] (.getExtractsResource unit-def (:metal resources)))
        mexes (filter (comp pos? get-extracts-metal get-def) team-units)
        mex-spots (->> mexes
                       (map
                         (fn [^Unit mex]
                           {(.getResourceMapSpotsNearest map-obj (:metal resources) (.getPos mex))
                            {(.getName (.getDef mex)) {:pos (.getPos mex) :obj mex}}}))
                       (apply merge-with merge))
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
                      "armcom" (fn [d] (string/starts-with? (unit/def-name d) "arm"))
                      "corcom" (fn [d] (string/starts-with? (unit/def-name d) "cor"))
                      identity)
        unit-defs (filter filter-side (.getUnitDefs callback))
        unit-defs-by-name (into {}
                            (map (juxt unit/def-name identity)
                                 unit-defs))
        unit-defs-by-type (->> unit-defs
                               (map (juxt unit/typeof identity))
                               (into {}))
        unit-def-names-by-type (->> unit-defs
                                    (map (juxt unit/typeof unit/def-name))
                                    (into {}))]
    {::economy (old/economy state)
     ::pois pois
     ::idle-units (filter-idle team-units)
     ::builders builder-units
     ::builders-data (map (partial unit-data resources) builder-units)
     ::team-units team-units
     ::metal-details metal-details
     ::mex-by-metal mex-spots
     ::unit-defs unit-defs
     ::unit-defs-by-name unit-defs-by-name
     ::unit-defs-by-type unit-defs-by-type
     ::unit-def-names-by-type unit-def-names-by-type}))


(defn give-commands
  [{:keys [^OOAICallback callback resources]} {::keys [idle-units mex-by-metal pois unit-defs-by-type]}]
  (let [^Map map-obj (.getMap callback)
        get-resource-map-spot-near (fn [^AIFloat3 pos]
                                     (when pos
                                       (.getResourceMapSpotsNearest map-obj (:metal resources) pos)))
        points (.getPoints map-obj false)
        _point-poses (set (map (fn [^Point point] (.getPosition point)) points))
        ^Game _game (.getGame callback)
        poi-by-unit (->> pois
                         (mapcat
                           (fn [poi]
                             (map
                               (fn [unit]
                                 [unit (:poi-dist unit)])
                               (:units poi)))))
        units-for-poi (atom {})
        get-closest-poi (fn [^Unit unit exclude]
                          (->> pois
                               (remove (comp (or exclude #{}) :poi-dist))
                               (filter (comp (unit/builds unit) poi/next-building))
                               (sort-by (comp (partial u/distance (.getPos unit)) :center))
                               first
                               :poi-dist))]
    #_ ; does nothing
    (doseq [poi pois]
      (let [center (:center poi)
            zone (rand-int 256)] ; why
        (.sendTextMessage game (str (:poi-dist poi) " z" zone) zone)
        (.setLastMessagePosition game center)))
    (doseq [^Unit unit idle-units]
      (when-not (get poi-by-unit unit)
        (cond
          (builder? unit)
          (when-let [closest-poi (get-closest-poi unit #{})]
            (log/debug "Unit" unit "(" (unit/typeof unit) ") is not in a POI"
                       "assigning it to closest that needs work which is" closest-poi)
            (swap! units-for-poi update closest-poi conj unit))
          (fighter? unit)
          (when-let [next-poi (rand-nth (vec pois))]
            (log/debug "Unit" unit "(" (unit/typeof unit) ") is not in a POI"
                       "sending it to fight at a random POI" (:poi-dist next-poi))
            (.moveTo unit (:center next-poi) (short 0) default-timeout)
            #_
            (if (pos? (rand-int 2))
              (.fight unit (:center next-poi) (short 0) fight-timeout)
              (.attackArea unit (:center next-poi) math/default-cluster-distance (short 0) fight-timeout)))
          :else
          nil)))
    (doseq [{:keys [poi-dist] :as poi} pois]
      (log/debug "POI" (:poi-dist poi) "at" (:center poi))
      (let [poi-wants (poi/next-building poi)
            idle-units (concat (get @units-for-poi poi-dist) (filter-idle (:units poi)))
            builders (filter builder? idle-units)
            fighters (->> idle-units
                          (remove builder?)
                          (filter fighter?))]
        (doseq [^Unit unit builders]
          (let [unit-builds (unit/builds unit)
                build-type (cond
                             (contains? unit-builds poi-wants) poi-wants
                             (unit/lab? unit)
                             (if (< 2 (count builders)) ; TODO better logic
                               ::cons-kbot
                               (rand-nth (vec unit-builds)))
                             :else
                             nil)
                build-def (get unit-defs-by-type build-type)
                min-dist (or (get min-build-dists build-type) 1)
                metal-radius (.getExtractorRadius map-obj (:metal resources))
                build-pos (cond
                            (= ::unit/mex build-type)
                            (->> poi
                                 :positions
                                 (remove (comp #(get mex-by-metal %) get-resource-map-spot-near))
                                 (map #(.findClosestBuildSite map-obj build-def % metal-radius 0 0))
                                 (filter #(.isPossibleToBuildAt map-obj build-def % 0))
                                 first)
                            build-def
                            (.findClosestBuildSite
                              map-obj build-def (:center poi) math/default-cluster-distance min-dist 0)
                            :else
                            nil)]
            (if (and build-type build-def build-pos)
              (do
                (log/debug "Unit" unit "which is an" (unit/def-name unit) "which is type"
                           (unit/typeof unit) "should build" build-type)
                (.build unit build-def build-pos 0 (short 0) default-timeout))
              (let [closest-poi (get-closest-poi unit #{(:poi-dist poi)})]
                (log/debug "Assigning unit" unit "(" (unit/typeof unit) ") to closest POI that needs work which is" closest-poi)
                (swap! units-for-poi update closest-poi conj unit)))))
        (doseq [^Unit unit fighters]
          (when-let [next-poi (rand-nth (vec pois))]
            (log/debug "Unit" unit "(" (unit/typeof unit) ") sent to fight at POI" (:poi-dist next-poi))
            (.moveTo unit (:center next-poi) (short 0) default-timeout)
            #_
            (if (pos? (rand-int 2))
              (.fight unit (:center next-poi) (short 0) fight-timeout)
              (.attackArea unit (:center next-poi) math/default-cluster-distance (short 0) fight-timeout))))))))


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
      (chime/periodic-seq now (jt/duration tick-millis :millis))
      (fn [_chimestamp]
        (run state))
      {:error-handler handle-error})))
