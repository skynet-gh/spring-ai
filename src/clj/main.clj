(ns main
  (:require
    [taoensso.timbre :as timbre :refer [debug info warn error fatal]]
    [taoensso.timbre.appenders.core :as appenders])
  (:import
    (com.springrts.ai AI))
  (:gen-class
    :name skynet.SpringAIMain
    :implements [com.springrts.ai.AI]
    :init constructor
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
  [[] (atom {})])


(defn ai-init [this skirmish-ai-id callback]
  (let [now (System/currentTimeMillis)
        logfile (.DataDirs_allocatePath callback (str "log-" now ".txt") true true false false)
        team-id (.SkirmishAI_getTeamId callback)]
    (timbre/merge-config!
      {:appenders
       {:println {:enabled? false}}
       :spit (appenders/spit-appender {:fname logfile})})
    (try
      (debug "Configured logging")
      (swap! (.state this) assoc :skirmish-ai-id skirmish-ai-id :callback callback :started now)
      (info "Id" skirmish-ai-id "team" team-id)
      (let [ids (range (.SkirmishAI_Info_getSize callback))
            info (zipmap (map #(.SkirmishAI_Info_getKey %) ids)
                         (map #(.SkirmishAI_Info_getVaue %) ids))]
        (swap! (.state this) assoc :info info)
        (info "Info map" info))
      (let [ids (range (.SkirmishAI_OptionValues_getSize callback))
            options (zipmap (map #(.SkirmishAI_OptionValues_getKey %) ids)
                            (map #(.SkirmishAI_OptionValues_getVaue %) ids))]
        (swap! (.state this) assoc :options options)
        (info "Options map" options))
      0
      (catch Exception e
        (fatal e "Error initializing")
        -1))))


(defn ai-update [this frame]
  (info "Update frame" frame)
  0)


(defn ai-release [this reason-code]
  (info "Releasing due to code" reason-code "which means" (release-reason reason-code))
  0)
