(ns skynet.core
  (:require
    [taoensso.timbre :as timbre :refer [debug info warn error fatal]]
    [taoensso.timbre.appenders.core :as appenders])
  (:import
    (com.springrts.ai AbstractAI AI))
  (:gen-class
    :extends com.springrts.ai.AbstractAI
    :implements [com.springrts.ai.AI]
    :init constructor
    :main false
    :name skynet.SkynetAIClj
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
  (.Log_log callback "init")
  (let [now (System/currentTimeMillis)
        fname (str "log-" now ".txt")
        logfile (.DataDirs_allocatePath callback fname true true false false)
        team-id (.SkirmishAI_getTeamId callback)]
    (.Log_log callback (str "logging to " logfile))
    (timbre/merge-config!
      {:appenders
       {:println {:enabled? false}
        :spit (appenders/spit-appender {:fname logfile :min-level :debug})}})
    (try
      (debug "Configured logging")
      (debug "Config dir" (.DataDirs_getConfigDir callback))
      (debug "Writeable dir" (.DataDirs_getWriteableDir callback))
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
      (let [ids (range (.SkirmishAI_Info_getSize callback))
            info-data (zipmap (map #(.SkirmishAI_Info_getKey callback %) ids)
                              (map #(.SkirmishAI_Info_getValue callback %) ids))]
        (swap! (.state this) assoc :info info-data)
        (info "Info map" info-data))
      (let [ids (range (.SkirmishAI_OptionValues_getSize callback))
            options (zipmap (map #(.SkirmishAI_OptionValues_getKey callback %) ids)
                            (map #(.SkirmishAI_OptionValues_getValue callback %) ids))]
        (swap! (.state this) assoc :options options)
        (info "Options map" options))
      (info "Init finished, state" @(.state this))
      0
      (catch Throwable e
        (error e "Throwable caught initializing")
        -1))))


(defn ai-unitFinished [this unit]
  (debug "Unit finished" unit)
  (debug "Unit finished" @(.state this))
  (let [callback (:callback @(.state this))
        unitdef (.Unit_getDef callback unit)]
    (debug "Unit def id" unitdef)
    (let [unitdefname (.UnitDef_getName callback unitdef)]
      (debug "Unit def name" unitdefname)
      (when (= "armcom" unitdefname)
        (.Game_sendTextMessage callback "Building solar panel with commander" 0)
        (info "Building solar panel with commander")
        (let [solardef (.getUnitDefByName callback "armsolar")
              compos (float-array 3)]
          (.Unit_getPos callback compos)
          (.Unit_build callback unit solardef compos 0 0 10000)))))
  0)


(defn ai-update [this frame]
  ;(trace "Update frame" frame)
  0)


(defn ai-release [this reason-code]
  (info "Releasing due to code" reason-code "which means" (get release-reason reason-code "n/a"))
  0)
