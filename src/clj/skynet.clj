(ns skynet
  (:require
    [chime.core :as chime]
    [clojure.core.protocols]
    [clojure.datafy :refer [datafy]]
    [java-time :as jt]
    [skynet.old :as old]
    [skynet.util :as u]
    [taoensso.timbre :as log])
  (:import
    (com.springrts.ai.oo.clb Unit)))


#_
(extend-protocol clojure.core.protocols/Datafiable
  Unit
  (datafy [x]
    {:use-metal (.getRes)}))


(defn unit-data [resources unit]
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


(defn world
  "Returns data about the world obtained from state."
  [{:keys [callback avg-wind resources metal-spots] :as state}]
  (let [map-obj (.getMap callback)
        team-units (.getTeamUnits callback)
        builder-units (filter (comp #(.isBuilder %) #(.getDef %)) team-units)
        mexes (filter (comp pos? #(.getExtractsResource % (:metal resources)) #(.getDef %)) team-units)
        mex-spots (->> mexes
                       (map
                         (fn [mex]
                           {(.getResourceMapSpotsNearest map-obj (.getPos mex))
                            {(.getName (.getdef mex)) mex}}))
                       (merge-with merge))
        metal-details (into {}
                        (map
                          (fn [pos]
                            [pos {::mexes (get mex-spots pos)}])
                          metal-spots))]
    {::economy (old/economy state)
     ::start-pos (.getStartPos map-obj)
     ::builders builder-units
     ::builders-data (map (partial unit-data resources) builder-units)
     ::team-units team-units
     ::metal-details metal-details}))


(defn run [state-atom]
  (log/info "Async planner")
  (log/info "Looking for idle units")
  (let [state @state-atom
        team-units (.getTeamUnits (:callback state))
        ;do-things-units (filter (comp #(or (.isBuilder %) (.isAbleToAttack %)) #(.getDef %)) team-units)
        do-things-units (remove (comp ignore-idle #(.getName %) #(.getDef %)) team-units)
        idle-units (filter (comp empty? #(.getCurrentCommands %)) do-things-units)]
    (log/debug (u/pprint-str (world state)))
    (dorun
      (map
        (fn [unit]
          (log/info "Giving" unit "type" (.getName (.getDef unit)) "a job")
          (old/assign-unit state unit))
        idle-units))))


(defn init [state]
  (log/info "Init async logic")
  (let [now (jt/instant)]
    (chime/chime-at
      (chime/periodic-seq now (jt/duration 1000 :millis))
      (fn [_chimestamp]
        (run state))
      {:error-handler handle-error})))
