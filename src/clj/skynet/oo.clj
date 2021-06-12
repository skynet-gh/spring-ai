(ns skynet.oo
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.string :as string]
    [taoensso.timbre :as timbre :refer [debug info warn error fatal]]
    [taoensso.timbre.appenders.core :as appenders])
  (:import
    (com.springrts.ai.oo AbstractOOAI AIFloat3 OOAI)
    (com.springrts.ai.oo.clb OOAICallback))
  (:gen-class
    :extends com.springrts.ai.oo.AbstractOOAI
    :init constructor
    :name skynet.SkynetAICljOO
    :prefix "ai-"
    :state state))


(def release-reason
  {0 "unspecified"
   1 "game ended"
   2 "team died"
   3 "AI killed"
   4 "AI crashed"
   5 "AI failed to init"
   6 "connection lost"
   7 "other reason"})

(def build-timeout 10000)
(def min-build-dist 5)


(defn pprint-str [d]
  (with-out-str
    (pprint d)))

(defmacro try-log
  "Try to execute body, catch Throwable, and log it."
  [fn-name & body]
  `(try
     ~@body
     (catch Throwable e#
       (error e# (str "Error in " ~fn-name))
       -1)))


(defn distance
  "Calculates the distance between two (AIFloat3) points."
  [a b]
  (let [xd (- (.x a) (.x b))
        yd (- (.y a) (.y b))
        zd (- (.z a) (.z b))]
    (+ (* xd xd) (* yd yd) (* zd zd))))

(defn discover-resources [state-atom callback]
  (info "Discovering resource positions")
  (let [resources (.getResources callback)
        _ (debug "Resources" resources)
        _ (debug "Resource names" (pr-str (map #(.getName %) resources)))
        metalres (first (filter (comp #{"Metal"} #(.getName %)) resources))
        _ (debug "Got metal res" metalres)
        energyres (first (filter (comp #{"Energy"} #(.getName %)) resources))
        _ (debug "Got energy res" energyres)
        metal-spots (seq (.. callback (getMap) (getResourceMapSpotsPositions metalres)))
        energy-spots (seq (.. callback (getMap) (getResourceMapSpotsPositions energyres)))
        resources {:resources {:metal metalres
                               :energy energyres}
                   :metal-spots metal-spots
                   :energy-spots energy-spots}]
    (info "Metal spots" (pr-str metal-spots))
    (info "Energy spots" (pr-str energy-spots))
    (swap! state-atom merge resources)
    (debug "Finished discovering resourses")
    resources))


(defn ai-constructor []
  [[] (atom {:units #{}})])

(defn ai-init [this skirmish-ai-id callback]
  (.. callback (getLog) (log "clojure init"))
  (let [now (System/currentTimeMillis)
        fname (str "log-" now ".txt")
        data-dirs (.getDataDirs callback)
        logfile (.allocatePath data-dirs fname true true false false)
        skirmish-ai (.getSkirmishAI callback)
        team-id (.getTeamId skirmish-ai)]
    (.. callback (getLog) (log (str "logging to " logfile)))
    (timbre/merge-config!
      {:appenders
       {:println {:enabled? false}
        :spit (appenders/spit-appender {:fname logfile :min-level :debug})}})
    (try-log "init"
      (debug "Configured logging")
      (debug "Config dir" (.getConfigDir data-dirs))
      (debug "Writeable dir" (.getWriteableDir data-dirs))
      (debug "Classpath" (System/getProperty "java.class.path"))
      (swap! (.state this) assoc :skirmish-ai-id skirmish-ai-id :callback callback :started now)
      (info "Initial state" @(.state this))
      (info "Id" skirmish-ai-id "team" team-id)
      (let [info-api (.getInfo skirmish-ai)
            ids (range (.getSize info-api))
            info-data (zipmap (map #(.getKey info-api %) ids)
                              (map #(.getValue info-api %) ids))]
        (swap! (.state this) assoc :info info-data)
        (info "Info map" info-data))
      (let [option-values (.getOptionValues skirmish-ai)
            ids (range (.getSize option-values))
            options (zipmap (map #(.getKey option-values %) ids)
                            (map #(.getValue option-values %) ids))]
        (swap! (.state this) assoc :options options)
        (info "Options map" options))
      (let [{:keys [resources]} (discover-resources (.state this) callback)
            unit-defs (.getUnitDefs callback)
            unit-defs-by-name (into {}
                                (map (juxt #(.getName %) identity)
                                     unit-defs))]
        (info (str "Got " (count unit-defs) " unit defs \n"
                   (pprint-str
                     (->> unit-defs
                          (mapv (fn [d] {:name (.getName d)
                                         :human-name (.getHumanName d)
                                         :tooltip (.getTooltip d)
                                         ;:tech-level (.getTechLevel d)  y u no work
                                         :metal-cost (.getCost d (:metal resources))
                                         :energy-cost (.getCost d (:energy resources))}))
                          (sort-by :name)))))
        (swap! (.state this) assoc :unit-defs unit-defs :unit-defs-by-name unit-defs-by-name))
      (info "Init finished")
      0)))

(defn economy-resource
  "Returns economy data for the given resource."
  [economy resource]
  {:current (.getCurrent economy resource)
   :excess (.getExcess economy resource)
   :income (.getIncome economy resource)
   :pull (.getPull economy resource)
   :received (.getReceived economy resource)
   :sent (.getSent economy resource)
   :share (.getShare economy resource)
   :storage (.getStorage economy resource)
   :usage (.getUsage economy resource)})

(defn economy
  "Returns all economy data"
  [{:keys [callback resources] :as state}]
  (let [economy (.getEconomy callback)
        {:keys [metal energy]} resources]
     {:metal (economy-resource economy metal)
      :energy (economy-resource economy energy)}))

(defn income-ratio
  "Returns ratio of resource income to pull, zero if no pull."
  [{:keys [income pull]}]
  (if (zero? pull)
    0
    (/ income pull)))

(defn sharing?
  "Returns true if the resource is at the share threshold."
  [{:keys [current share storage]}]
  (< share (/ current storage)))


(defn assign-unit
  "A unit has finished being constructed, or is now idle. Attempt to do something with it."
  [this unit]
  (let [unitdefname (.. unit (getDef) (getName))
        {:keys [callback] :as state} @(.state this)]
    (when (#{"armlab"} unitdefname)
      (let [buildname "armck"
            builddef (.. callback (getUnitDefByName buildname))
            unitpos (.getPos unit)]
        (info "Building" (.getName builddef) "with" unitdefname "at" unitpos)
        (.build unit builddef unitpos 0 (short 0) build-timeout)))
    (when (#{"armalab"} unitdefname)
      (let [buildname "armack"
            builddef (.. callback (getUnitDefByName buildname))
            unitpos (.getPos unit)]
        (info "Building" (.getName builddef) "with" unitdefname "at" unitpos)
        (.build unit builddef unitpos 0 (short 0) build-timeout)))
    (when (#{"armcom" "armck" "armack"} unitdefname) ; TODO any builder
      (let [{:keys [resources metal-spots]} state
            {:keys [metal energy] :as economy} (economy state)
            metal-income-ratio (income-ratio metal)
            energy-income-ratio (income-ratio energy)
            team-units (.getTeamUnits callback)
            _ (info (count team-units) "units on team")
            _ (info (str "Economy data \n" (pprint-str economy)))
            _ (info "Metal income ratio to pull is" metal-income-ratio "vs energy income ratio" energy-income-ratio)
            should-build-metal (<= metal-income-ratio energy-income-ratio)
            map-obj (.getMap callback)
            unitpos (.getPos unit)
            metal-spot (when should-build-metal
                         ; (.getResourceMapSpotsNearest map-obj (:metal resources) unitpos))
                         (let [metal-radius (.getExtractorRadius map-obj (:metal resources))
                               buildname (if (= "armack" unitdefname)
                                            "armmoho"
                                            "armmex")
                               builddef (.. callback (getUnitDefByName buildname))
                               mex-search (if (= "armack" unitdefname) 40 20)]
                           (debug "Metal radius is" metal-radius)
                           (->> metal-spots
                                (sort-by (fn [p] (distance unitpos (AIFloat3. (.x p) 0 (.z p))))) ; y is resource
                                (map #(.findClosestBuildSite map-obj builddef % mex-search 0 0))
                                (filter #(.isPossibleToBuildAt map-obj builddef % 0))
                                first)))
            should-build-metal-storage (and (= "armck" unitdefname) (sharing? metal))
            should-build-energy-storage (and (= "armck" unitdefname) (sharing? energy))
            should-build-converter (< 70 (:excess energy))
            kbot-lab (first (filter (comp #{"armlab"} #(.getName %) #(.getDef %)) team-units))
            akbot-lab (first (filter (comp #{"armalab"} #(.getName %) #(.getDef %)) team-units))
            buildname (cond
                        (not kbot-lab)
                        "armlab"
                        (and (not akbot-lab) (= "armck" unitdefname))
                        "armalab"
                        should-build-energy-storage
                        "armestor"
                        should-build-metal-storage
                        "armmstor"
                        should-build-converter
                        (if (= "armack" unitdefname)
                          "armmmkr"
                          "armmakr")
                        (and should-build-metal metal-spot)
                        (if (= "armack" unitdefname)
                          "armmoho"
                          "armmex")
                        :else
                        (case unitdefname
                          "armcom"
                          "armsolar"
                          "armck"
                          "armadvsol"
                          "armack"
                          "armdf"))
            builddef (.. callback (getUnitDefByName buildname))
            _ (when-not builddef
                (warn "Unable to find unit definition for" buildname))
            pos (or metal-spot
                    (.findClosestBuildSite map-obj builddef unitpos 1000 min-build-dist 0))]
        (info "Building" (.getName builddef) "with" unitdefname "at" pos)
        (.build unit builddef pos 0 (short 0) build-timeout)))))


(defn ai-unitFinished [this unit]
  (try-log "unitFinished"
    (let [unitdefname (.. unit (getDef) (getName))]
      (debug "Unit" (str "'" unitdefname "'") "finished")
      (assign-unit this unit))
    0))


(defn ai-unitIdle [this unit]
  (try-log "unitIdle"
    (let [unitdefname (.. unit (getDef) (getName))]
      (info "Unit" (str "'" unitdefname "'") "idle")
      (assign-unit this unit))
    0))

(defn ai-unitCreated [this unit builder]
  (try-log "unitCreated"
    (let [unitdefname (.. unit (getDef) (getName))]
      (info "Unit" (str "'" unitdefname "'") "built by"
            (when builder
              (when-let [builderdef (.getDef builder)]
                (str "'" (.getName builderdef) "'")))))
    0))

(defn ai-unitMoveFailed [this unit]
  (try-log "unitMoveFailed"
    (let [unitdefname (.. unit (getDef) (getName))]
      (info "Unit" (str "'" unitdefname "'") "move failed"))
    0))

(defn ai-unitDamaged [this unit attacker damage direction weapon-def paralyzer]
  (try-log "unitDamaged"
    (info "Unit" (str "'" (.. unit (getDef) (getName)) "'") "damaged by"
          (when attacker
            (when-let [attackerdef (.getDef attacker)]
              (.getName attackerdef)))
          "for" damage "from" direction "using" weapon-def ", paralyzer" paralyzer)
    0))

(defn ai-unitDestroyed [unit attacker]
  (try-log "unitDestroyed"
    (let [unitdefname (.. unit (getDef) (getName))
          attackerdefname (.. attacker (getDef) (getName))]
      (info "Unit" (str "'" unitdefname "'") "destroyed by" attackerdefname))
    0))

(defn ai-enemyDamaged [this enemy attacker damage direction weapon-def paralyzer]
  (try-log "enemyDamaged"
    (let [enemydefname (.. enemy (getDef) (getName))
          attackerdefname (.. attacker (getDef) (getName))]
      (info "Enemy" (str "'" enemydefname "'") "damaged by" attackerdefname "for" damage
            "from" direction "using" weapon-def ", paralyzer" paralyzer))
    0))

(defn ai-enemyDestroyed [this enemy attacker]
  (try-log "enemyDestroyed"
    (let [enemydefname (.. enemy (getDef) (getName))
          attackerdefname (.. attacker (getDef) (getName))]
      (info "Enemy" (str "'" enemydefname "'") "destroyed by" attackerdefname))
    0))

(defn ai-enemyEnterRadar [this enemy]
  (try-log "enemyEnterRadar"
    (info "Enemy" (when enemy
                    (when-let [enemydef (.getDef enemy)]
                      "'" (.getName enemydef) "'"))
          "entered radar")
    0))

(defn ai-enemyLeaveRadar [this enemy]
  (try-log "enemyLeaveRadar"
    (let [enemydefname (.. enemy (getDef) (getName))]
      (info "Enemy" (str "'" enemydefname "'") "left radar"))
    0))

(defn ai-enemyEnterLOS [this enemy]
  (try-log "enemyEnterLOS"
    (let [enemydefname (.. enemy (getDef) (getName))]
      (info "Enemy" (str "'" enemydefname "'") "entered LOS"))
    0))

(defn ai-enemyLeaveLOS [this enemy]
  (try-log "enemyLeaveLOS"
    (let [enemydefname (.. enemy (getDef) (getName))]
      (info "Enemy" (str "'" enemydefname "'") "left LOS"))
    0))

(defn ai-enemyCreated [this enemy]
  (try-log "enemyCreated"
    (let [enemydefname (.. enemy (getDef) (getName))]
      (info "Enemy" (str "'" enemydefname "'") "created"))
    0))

(defn ai-enemyFinished [this enemy]
  (try-log "enemyFinished"
    (let [enemydefname (.. enemy (getDef) (getName))]
      (info "Enemy" (str "'" enemydefname "'") "finished"))
    0))

(defn ai-weaponFired [this unit weapon-def]
  (try-log "weaponFired"
    (let [unitdefname (.. unit (getDef) (getName))]
      (info "Unit" (str "'" unitdefname "'") "fired weapon" weapon-def))
    0))

(defn ai-playerCommand [this units command-topic-id player-id]
  (try-log "playerCommand"
    (info "Player" player-id "gave command" command-topic-id "to" (count units) "units")
    0))

(defn ai-commandFinished [this unit command-id command-topic-id]
  (try-log "commandFinished"
    (let [unitdefname (.. unit (getDef) (getName))]
      (info "Command" command-id "topic" command-topic-id "for unit" (str "'" unitdefname "'") "finished"))
    0))

(defn ai-seismicPing [this pos strength]
  (try-log "seismicPing"
    (info "Seismic ping at" pos "strength" strength)
    0))

(defn ai-message [this player message]
  (try-log "message"
    (info "Player" player "sent message" (str "'" message "'"))
    0))

(defn ai-update [this frame]
  (try-log "update"
    0))

(defn ai-release [this reason-code]
  (try-log "release"
    (info "Releasing due to code" reason-code "which means" (get release-reason reason-code "n/a"))
    0))