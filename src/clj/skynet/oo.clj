(ns skynet.oo
  (:require
    [taoensso.timbre :as timbre :refer [debug info warn error fatal]]
    [taoensso.timbre.appenders.core :as appenders])
  (:import
    (com.springrts.ai.oo OOAI)
    (com.springrts.ai.oo AbstractOOAI)
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
    (try
      (debug "Configured logging")
      (debug "Config dir" (.getConfigDir data-dirs))
      (debug "Writeable dir" (.getWriteableDir data-dirs))
      (debug "Classpath" (System/getProperty "java.class.path"))
      (try
        (when-let [clazz (Class/forName "skynet.SkynetAIClj")]
          (let [classloader (.getClassLoader clazz)]
            (info "ClassLoader" classloader "of type" (type classloader))))
        (catch Exception e
          (warn e "Could not find class")))
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
      (info "Init finished, state" @(.state this))
      0
      (catch Throwable e
        (error e "Throwable caught initializing")
        -1))))


(defn ai-unitFinished [this unit]
  (let [unitdefname (.. unit (getDef) (getName))]
    (debug "Unit" (str "'" unitdefname "'") "finished")
    (when (= "armcom" unitdefname)
      (let [callback (:callback @(.state this))]
        (let [solardef (.. callback (getUnitDefByName "armsolar"))]
          (info "Building" solardef "with" unitdefname)
          (.build unit solardef (.getPos unit) 0 (short 0) Integer/MAX_VALUE)))))
  0)


(defn ai-unitIdle [this unit]
  (let [unitdefname (.. unit (getDef) (getName))]
    (info "Unit" (str "'" unitdefname "'") "idle")
    (when (= "armcom" unitdefname)
      (let [callback (:callback @(.state this))]
        (let [solardef (.. callback (getUnitDefByName "armsolar"))]
          (info "Building" solardef "with" unitdefname)
          (.build unit solardef (.getPos unit) 0 (short 0) Integer/MAX_VALUE)))))
  0)

(defn ai-unitCreated [this unit builder]
  (let [unitdefname (.. unit (getDef) (getName))
        builderdefname (.. builder (getDef) (getName))]
    (info "Unit" (str "'" unitdefname "'") "built by" (str "'" builderdefname "'"))))

(defn ai-unitMoveFailed [this unit]
  (let [unitdefname (.. unit (getDef) (getName))]
    (info "Unit" (str "'" unitdefname "'") "move failed"))
  0)

(defn ai-unitDamaged [this unit attacker damage direction weapon-def paralyzer]
  (let [unitdefname (.. unit (getDef) (getName))
        attackerdefname (.. attacker (getDef) (getName))]
    (info "Unit" (str "'" unitdefname "'") "damaged by" attackerdefname "for" damage
          "from" direction "using" weapon-def ", paralyzer" paralyzer))
  0)

(defn ai-unitDesttroyed [unit attacker]
  (let [unitdefname (.. unit (getDef) (getName))
        attackerdefname (.. attacker (getDef) (getName))]
    (info "Unit" (str "'" unitdefname "'") "destroyed by" attackerdefname))
  0)

(defn ai-enemyDamaged [this enemy attacker damage direction weapon-def paralyzer]
  (let [enemydefname (.. enemy (getDef) (getName))
        attackerdefname (.. attacker (getDef) (getName))]
    (info "Enemy" (str "'" enemydefname "'") "damaged by" attackerdefname "for" damage
          "from" direction "using" weapon-def ", paralyzer" paralyzer))
  0)

(defn ai-enemyDesttroyed [enemy attacker]
  (let [enemydefname (.. enemy (getDef) (getName))
        attackerdefname (.. attacker (getDef) (getName))]
    (info "Enemy" (str "'" enemydefname "'") "destroyed by" attackerdefname))
  0)

(defn ai-enemyEnterRadar [enemy]
  (let [enemydefname (.. enemy (getDef) (getName))]
    (info "Enemy" (str "'" enemydefname "'") "entered radar"))
  0)

(defn ai-enemyLeaveRadar [enemy]
  (let [enemydefname (.. enemy (getDef) (getName))]
    (info "Enemy" (str "'" enemydefname "'") "left radar"))
  0)

(defn ai-enemyEnterLOS [enemy]
  (let [enemydefname (.. enemy (getDef) (getName))]
    (info "Enemy" (str "'" enemydefname "'") "entered LOS"))
  0)

(defn ai-enemyLeaveLOS [enemy]
  (let [enemydefname (.. enemy (getDef) (getName))]
    (info "Enemy" (str "'" enemydefname "'") "left LOS"))
  0)

(defn ai-enemyCreated [enemy]
  (let [enemydefname (.. enemy (getDef) (getName))]
    (info "Enemy" (str "'" enemydefname "'") "created"))
  0)

(defn ai-enemyFinished [enemy]
  (let [enemydefname (.. enemy (getDef) (getName))]
    (info "Enemy" (str "'" enemydefname "'") "finished"))
  0)

(defn ai-weaponFired [unit weapon-def]
  (let [unitdefname (.. unit (getDef) (getName))]
    (info "Unit" (str "'" unitdefname "'") "fired weapon" weapon-def))
  0)

(defn ai-playerCommand [units command-topic-id player-id]
  (info "Player" player-id "gave command" command-topic-id "to" (count units) "units")
  0)

(defn ai-commandFinished [unit command-id command-topic-id]
  (let [unitdefname (.. unit (getDef) (getName))]
    (info "Command" command-id "topic" command-topic-id "for unit" (str "'" unitdefname "'") "finished"))
  0)

(defn ai-seismicPing [pos strength]
  (info "Seismic ping at" pos "strength" strength)
  0)

(defn ai-message [player message]
  (info "Player" player "sent message" (str "'" message "'"))
  0)


(defn ai-update [this frame]
  (let [state @(.state this)
        {:keys [units]} state
        {:keys [commander]} units]
    nil)
  0)

(defn ai-message [this player message]
  (info "Message from player" player ":" message)
  0)


(defn ai-release [this reason-code]
  (info "Releasing due to code" reason-code "which means" (get release-reason reason-code "n/a"))
  0)
