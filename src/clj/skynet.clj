(ns skynet
  (:require
    [chime.core :as chime]
    [skynet.old :as old]
    [java-time :as jt]
    [taoensso.timbre :as log]))


(defn handle-error [exception]
  (log/error exception "An error occurred in async")
  true)


(defn run [state-atom]
  (log/info "Async planner")
  (log/info "Looking for idle units")
  (let [state @state-atom
        team-units (.getTeamUnits (:callback state))
        idle-units (filter (comp empty? #(.getCurrentCommands %)) team-units)]
    (dorun
      (map
        (fn [unit]
          (log/info "Giving" unit "a job")
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
