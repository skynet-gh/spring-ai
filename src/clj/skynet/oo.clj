(ns skynet.oo
  (:require
    [skynet]
    [skynet.math :as math]
    [skynet.util :as u]
    [taoensso.timbre :as log]
    [taoensso.timbre.appenders.core :as appenders])
  (:import
    (com.springrts.ai.oo AIFloat3))
  (:gen-class
    :extends com.springrts.ai.oo.AbstractOOAI
    :init constructor
    :name skynet.SkynetAICljOO
    :prefix "ai-"
    :state state))


(set! *warn-on-reflection* true)


(def release-reason
  {0 "unspecified"
   1 "game ended"
   2 "team died"
   3 "AI killed"
   4 "AI crashed"
   5 "AI failed to init"
   6 "connection lost"
   7 "other reason"})


(defn discover-resources [state-atom callback]
  (log/info "Discovering resource positions")
  (let [resources (.getResources callback)
        _ (log/debug "Resources" resources)
        _ (log/debug "Resource names" (pr-str (map #(.getName %) resources)))
        metalres (first (filter (comp #{"Metal"} #(.getName %)) resources))
        _ (log/debug "Got metal res" metalres)
        energyres (first (filter (comp #{"Energy"} #(.getName %)) resources))
        _ (log/debug "Got energy res" energyres)
        map-obj (.getMap callback)
        metal-spots (seq (.getResourceMapSpotsPositions map-obj metalres))
        with-elevation (fn [pos]
                         (let [x (.x pos)
                               z (.z pos)]
                           (AIFloat3. x (.getElevationAt map-obj x z) z)))
        metal-positions (map with-elevation metal-spots)
        metal-clusters (let [clusters (math/cluster metal-positions)]
                         (map-indexed
                           (fn [n cluster]
                             {:cluster n
                              :center (math/average cluster)
                              :positions cluster})
                           clusters))
        energy-spots (seq (.getResourceMapSpotsPositions map-obj energyres))
        energy-positions (map with-elevation energy-spots)
        min-wind (.getMinWind map-obj)
        max-wind (.getMaxWind map-obj)
        avg-wind (/ (+ min-wind max-wind) 2)
        resources {:resources {:metal metalres
                               :energy energyres}
                   :metal-spots metal-spots
                   :metal-positions metal-positions
                   :metal-clusters metal-clusters
                   :energy-spots energy-spots
                   :energy-positions energy-positions
                   :min-wind min-wind
                   :avg-wind avg-wind
                   :max-wind max-wind}]
    (log/info "Metal spots" (pr-str metal-spots))
    (log/info "Energy spots" (pr-str energy-spots))
    (swap! state-atom merge resources)
    (log/debug "Finished discovering resourses")
    resources))


(defn ai-constructor []
  [[] (atom {})])

(defn ai-init [this skirmish-ai-id callback]
  (.. callback (getLog) (log "clojure init"))
  (let [now (System/currentTimeMillis)
        skirmish-ai (.getSkirmishAI callback)
        team-id (.getTeamId skirmish-ai)
        fname (str "log-" now ".txt")
        data-dirs (.getDataDirs callback)
        logfile (.allocatePath data-dirs fname true true false false)]
    (.. callback (getLog) (log (str "logging to " logfile)))
    (log/merge-config!
      {:appenders
       {:println {:enabled? false}
        :spit (appenders/spit-appender {:fname logfile :min-level :debug})}
       :output-fn (partial log/default-output-fn {:stacktrace-fonts {}})})
    (u/try-log "init"
      (log/debug "Configured logging")
      (log/debug "Config dir" (.getConfigDir data-dirs))
      (log/debug "Writeable dir" (.getWriteableDir data-dirs))
      (log/debug "Classpath" (System/getProperty "java.class.path"))
      (swap! (.state this) assoc :skirmish-ai-id skirmish-ai-id :callback callback :started now)
      (log/info "Initial state" @(.state this))
      (log/info "Id" skirmish-ai-id "team" team-id)
      (let [info-api (.getInfo skirmish-ai)
            ids (range (.getSize info-api))
            info-data (zipmap (map #(.getKey info-api %) ids)
                              (map #(.getValue info-api %) ids))]
        (swap! (.state this) assoc :info info-data)
        (log/info "Info map" info-data))
      (let [option-values (.getOptionValues skirmish-ai)
            ids (range (.getSize option-values))
            options (zipmap (map #(.getKey option-values %) ids)
                            (map #(.getValue option-values %) ids))]
        (swap! (.state this) assoc :options options)
        (log/info "Options map" options))
      (let [{:keys [resources]} (discover-resources (.state this) callback)
            unit-defs (.getUnitDefs callback)
            unit-defs-by-name (into {}
                                (map (juxt #(.getName %) identity)
                                     unit-defs))]
        (log/info
          (str "Got " (count unit-defs) " unit defs \n"
               (u/pprint-str
                 (->> unit-defs
                      (mapv (fn [d] {:name (.getName d)
                                     :human-name (.getHumanName d)
                                     :tooltip (.getTooltip d)
                                     ;:tech-level (.getTechLevel d)  y u no work
                                     :metal-cost (.getCost d (:metal resources))
                                     :energy-cost (.getCost d (:energy resources))}))
                      (sort-by :name)))))
        (swap! (.state this) assoc :unit-defs unit-defs :unit-defs-by-name unit-defs-by-name))
      (swap! (.state this) assoc :chimer (skynet/init (.state this)))
      (log/info "Init finished")
      0)))


(defn ai-unitFinished [_this unit]
  (u/try-log "unitFinished"
    (let [unitdefname (.. unit (getDef) (getName))]
      (log/debug "Unit" (str "'" unitdefname "'") "finished"))
      ;(old/assign-unit this unit))
    0))


(defn ai-unitIdle [_this unit]
  (u/try-log "unitIdle"
    (let [unitdefname (.. unit (getDef) (getName))]
      (log/info "Unit" (str "'" unitdefname "'") "idle"))
      ;(old/assign-unit this unit))
    0))

(defn ai-unitCreated [_this _unit _builder]
  0
  #_
  (u/try-log "unitCreated"
    (let [unitdefname (.. unit (getDef) (getName))]
      (info "Unit" (str "'" unitdefname "'") "built by"
            (when builder
              (when-let [builderdef (.getDef builder)]
                (str "'" (.getName builderdef) "'")))))
    0))

(defn ai-unitMoveFailed [_this unit]
  (u/try-log "unitMoveFailed"
    (let [unitdefname (.. unit (getDef) (getName))]
      (log/info "Unit" (str "'" unitdefname "'") "move failed"))
      ;(old/assign-unit this unit))
    0))

(defn ai-unitDamaged [_this _unit _attacker _damage _direction _weapon-def _paralyzer]
  0
  #_
  (try-log "unitDamaged"
    (trace "Unit" (str "'" (.. unit (getDef) (getName)) "'") "damaged by"
           (when attacker
             (when-let [attackerdef (.getDef attacker)]
               (str "'" (.getName attackerdef) "'")))
           "for" damage "from" direction "using" weapon-def ", paralyzer" paralyzer)
    0))

(defn ai-unitDestroyed [_unit _attacker]
  0
  #_
  (try-log "unitDestroyed"
    (info "Unit"
          (when unit
            (when-let [unitdef (.getDef unit)]
              (str "'" (.getName unitdef) "'")))
          "destroyed by"
          (when attacker
            (when-let [attackerdef (.getDef attacker)]
              (str "'" (.getName attackerdef) "'"))))
    0))

(defn ai-enemyDamaged [_this _enemy _attacker _damage _direction _weapon-def _paralyzer]
  0
  #_
  (try-log "enemyDamaged"
    (trace "Enemy"
           (when enemy
             (when-let [enemydef (.getDef enemy)]
               (str "'" (.getName enemydef) "'")))
           "damaged by"
           (when attacker
             (when-let [attackerdef (.getDef attacker)]
               (str "'" (.getName attackerdef) "'")))
           "for" damage
           "from" direction "using" weapon-def ", paralyzer" paralyzer)
    0))

(defn ai-enemyDestroyed [_this _enemy _attacker]
  0
  #_
  (try-log "enemyDestroyed"
    (info "Enemy"
          (when enemy
            (when-let [enemydef (.getDef enemy)]
              (str "'" (.getName enemydef) "'")))
          "destroyed by"
          (when attacker
            (when-let [attackerdef (.getDef attacker)]
              (.getName attackerdef))))
    0))

(defn ai-enemyEnterRadar [_this _enemy]
  0
  #_
  (try-log "enemyEnterRadar"
    (info "Enemy" (when enemy
                    (when-let [enemydef (.getDef enemy)]
                      (str "'" (.getName enemydef) "'")))
          "entered radar")
    0))

(defn ai-enemyLeaveRadar [_this _enemy]
  0
  #_
  (try-log "enemyLeaveRadar"
    (info "Enemy" (str "'" (when enemy
                             (when-let [enemydef (.getDef enemy)]
                               (.getName enemydef)))
                       "'")
          "left radar")
    0))

(defn ai-enemyEnterLOS [_this _enemy]
  0
  #_
  (try-log "enemyEnterLOS"
    (let [enemydefname (.. enemy (getDef) (getName))]
      (info "Enemy" (str "'" enemydefname "'") "entered LOS"))
    0))

(defn ai-enemyLeaveLOS [_this _enemy]
  0
  #_
  (try-log "enemyLeaveLOS"
    (let [enemydefname (.. enemy (getDef) (getName))]
      (info "Enemy" (str "'" enemydefname "'") "left LOS"))
    0))

(defn ai-enemyCreated [_this _enemy]
  0
  #_
  (try-log "enemyCreated"
    (let [enemydefname (.. enemy (getDef) (getName))]
      (info "Enemy" (str "'" enemydefname "'") "created"))
    0))

(defn ai-enemyFinished [_this _enemy]
  0
  #_
  (try-log "enemyFinished"
    (let [enemydefname (.. enemy (getDef) (getName))]
      (info "Enemy" (str "'" enemydefname "'") "finished"))
    0))

(defn ai-weaponFired [_this _unit _weapon-def]
  0
  #_
  (u/try-log "weaponFired"
    (let [unitdefname (.. unit (getDef) (getName))]
      (log/trace "Unit" (str "'" unitdefname "'") "fired weapon" weapon-def))
    0))

(defn ai-playerCommand [_this _units _command-topic-id _player-id]
  0
  #_
  (u/try-log "playerCommand"
    (log/info "Player" player-id "gave command" command-topic-id "to" (count units) "units")
    0))

(defn ai-commandFinished [_this _unit _command-id _command-topic-id]
  0
  #_
  (u/try-log "commandFinished"
    (let [unitdefname (.. unit (getDef) (getName))]
      (log/info "Command" command-id "topic" command-topic-id "for unit" (str "'" unitdefname "'") "finished"))
    0))

(defn ai-seismicPing [_this _pos _strength]
  0
  #_
  (u/try-log "seismicPing"
    (log/info "Seismic ping at" pos "strength" strength)
    0))

(defn ai-message [_this _player _message]
  0
  #_
  (u/try-log "message"
    (log/info "Player" player "sent message" (str "'" message "'"))
    0))

(defn ai-update [_this _frame]
  0
  #_
  (u/try-log "update"
    0))

(defn ai-release [this reason-code]
  (u/try-log "release"
    (log/info "Releasing due to code" reason-code "which means" (get release-reason reason-code "n/a"))
    (when-let [chimer (-> (.state this) deref :chimer)]
      (log/info "Stopping async job")
      (.close chimer))
    0)
  0)
