(ns skynet.old
  (:require
    [skynet.util :as u]
    [taoensso.timbre :as log])
  (:import
    (com.springrts.ai.oo AIFloat3)))


(def build-timeout 10000)
(def min-build-dist 5)
(def min-build-dists
  {"armcom" 20
   "armck" 30
   "armack" 10})
(def build-dist-override
  {"armmakr" 40
   "armnanotc" 5})


(def guard-a-lab-chance 0) ;0.01)
(def repair-something-chance 0.5)
(def help-build-something-chance 0.50)
(def t2-help-build-something-chance 0) ;0.10)


(def t1-kbots
  ["armck" "armpw" "armham" "armrock" "armwar"])

(def t2-kbots
  ["armack" "armfido" "armfboy"])

(def t3-kbots
  ["armbanth"])

(def any-combat
  (disj
    (set
      (concat
        t1-kbots
        t2-kbots
        t3-kbots))
    "armck" "armack"))


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
  [{:keys [callback resources]}]
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

(defn build-something
  [unit {:keys [callback] :as state} team-units unitdefname]
  (let [{:keys [avg-wind resources metal-spots]} state
        {:keys [metal energy] :as economy} (economy state)
        metal-income-ratio (income-ratio metal)
        energy-income-ratio (income-ratio energy)
        _ (log/info (count team-units) "units on team")
        _ (log/info (str "Economy data \n" (u/pprint-str economy)))
        _ (log/info "Metal income ratio to pull is" metal-income-ratio "vs energy income ratio" energy-income-ratio)
        should-build-metal (and (not= "armcom" unitdefname)
                                (<= metal-income-ratio energy-income-ratio))
        map-obj (.getMap callback)
        unitpos (.getPos unit)
        metal-spot (let [metal-radius (.getExtractorRadius map-obj (:metal resources))
                         buildname (if (= "armack" unitdefname)
                                      "armmoho"
                                      "armmex")
                         builddef (.. callback (getUnitDefByName buildname))
                         mex-search (if (= "armack" unitdefname) 50 10)]
                     (log/debug "Metal radius is" metal-radius)
                     (->> metal-spots
                          (sort-by (fn [p] (u/distance unitpos (AIFloat3. (.x p) 0 (.z p))))) ; y is resource
                          (map #(.findClosestBuildSite map-obj builddef % mex-search 0 0))
                          (filter #(.isPossibleToBuildAt map-obj builddef % 0))
                          first))
        should-build-metal-storage (and (#{"armcom" "armck"} unitdefname) (sharing? metal))
        should-build-energy-storage (and (#{"armcom" "armck"} unitdefname)
                                         (or (sharing? energy)
                                             (< (:storage energy) (* (:income energy) 5))))
        should-build-converter (or should-build-energy-storage
                                   (pos? (- (:income energy) (:pull energy))))
        mex (first (filter (comp #{"armmex"} #(.getName %) #(.getDef %)) team-units))
        kbot-labs (filter (comp #{"armlab"} #(.getName %) #(.getDef %)) team-units)
        kbot-lab (first kbot-labs)
        akbot-labs (filter (comp #{"armalab"} #(.getName %) #(.getDef %)) team-units)
        akbot-lab (first akbot-labs)
        gantry (first (filter (comp #{"armshltx"} #(.getName %) #(.getDef %)) team-units))
        buildname (cond
                    (and (not mex) metal-spot)
                    (if (= "armack" unitdefname)
                      "armmoho"
                      "armmex")
                    (or (not kbot-lab)
                        (< (count kbot-labs)
                           (quot (:income metal) 20)))
                    "armlab"
                    (and should-build-metal metal-spot)
                    (if (= "armack" unitdefname)
                      "armmoho"
                      "armmex")
                    (or (< (count akbot-labs)
                           (quot (:income metal) 50))
                        (and (not akbot-lab)
                             (or (< 20 (:income metal)) (< 1000 (:current metal)))
                             (or (< 500 (:income energy)) (< 5000 (:current energy)))
                             (= "armck" unitdefname)))
                    "armalab"
                    (and (not gantry)
                         (or (< 100 (:income metal)) (< 5000 (:current metal)))
                         (or (< 2000 (:income energy)) (< 10000 (:current energy)))
                         (= "armack" unitdefname))
                    "armshltx"
                    (and (= "armck" unitdefname)
                         (sharing? metal))
                    "armnanotc"
                    should-build-converter
                    (if (= "armack" unitdefname)
                      "armmmkr"
                      "armmakr")
                    should-build-energy-storage "armestor"
                    should-build-metal-storage "armmstor"
                    :else
                    (case unitdefname
                      "armcom"
                      (if (< avg-wind 5)
                        "armsolar"
                        (rand-nth ["armsolar" "armwin"]))
                      "armck"
                      "armadvsol"
                      "armack"
                      "armfus"))
        builddef (.. callback (getUnitDefByName buildname))
        _ (when-not builddef
            (log/warn "Unable to find unit definition for" buildname))
        build-dist (get min-build-dists unitdefname
                        (get build-dist-override buildname min-build-dist))
        source-pos (or (when (= "armnanotc" buildname)
                         (cond
                           gantry (.getPos gantry)
                           akbot-lab (.getPos kbot-lab)
                           kbot-lab (.getPos kbot-lab)))
                       unitpos)
        pos (or (when should-build-metal metal-spot)
                (.findClosestBuildSite map-obj builddef source-pos 5000 build-dist 0))]
    (log/info "Building" (.getName builddef) "with" unitdefname "at" pos)
    (.build unit builddef pos 0 (short 0) build-timeout)))

(defn guard-a-lab [unit _state team-units]
  (let [targets (filterv (comp #{"armlab" "armalab" "armshltx"} #(.getName %) #(.getDef %)) team-units)]
    (if (seq targets)
      (do
        (.guard unit (rand-nth targets) (short 0) Integer/MAX_VALUE)
        true)
      false)))

(defn repair-something [_this unit _state team-units]
  (let [needs-repair (filterv (fn [u]
                                (< (.getHealth u) (.getMaxHealth u)))
                              team-units)]
    (if (seq needs-repair)
      (do
        (.repair unit (rand-nth needs-repair) (short 0) Integer/MAX_VALUE)
        true)
      false)))

(defn help-build-something [unit _state team-units]
  (let [building (filterv #(.isBeingBuilt %) team-units)]
    (if (seq building)
      (do
        (.repair unit (rand-nth building) (short 0) Integer/MAX_VALUE)
        true)
      false)))

(defn assign-unit
  "A unit has finished being constructed, or is now idle. Attempt to do something with it."
  [{:keys [callback] :as state} unit]
  (let [unitdefname (.. unit (getDef) (getName))
        team-units (.getTeamUnits callback)
        {:keys [metal _energy]} (economy state)
        ckbot (first (filter (comp #{"armck"} #(.getName %) #(.getDef %)) team-units))
        ackbot (first (filter (comp #{"armack"} #(.getName %) #(.getDef %)) team-units))]
    (cond
      (#{"armlab"} unitdefname)
      (let [buildname (if (and ckbot (not (sharing? metal)))
                        (rand-nth t1-kbots)
                        "armck")
            builddef (.. callback (getUnitDefByName buildname))
            unitpos (.getPos unit)]
        (log/info "Building" builddef "(" buildname ") with" unitdefname "at" unitpos)
        (.build unit builddef unitpos 0 (short 0) build-timeout))
      (#{"armalab"} unitdefname)
      (let [buildname (if (and ackbot (not (sharing? metal)))
                        (rand-nth t2-kbots)
                        "armack")
            builddef (.. callback (getUnitDefByName buildname))
            unitpos (.getPos unit)]
        (log/info "Building" (.getName builddef) "with" unitdefname "at" unitpos)
        (.build unit builddef unitpos 0 (short 0) build-timeout))
      (#{"armshltx"} unitdefname)
      (let [buildname (rand-nth t3-kbots)
            builddef (.. callback (getUnitDefByName buildname))
            unitpos (.getPos unit)]
        (log/info "Building" (.getName builddef) "with" unitdefname "at" unitpos)
        (.build unit builddef unitpos 0 (short 0) build-timeout))
      (#{"armcom" "armck"} unitdefname)
      (when (or (> (rand) help-build-something-chance)
                (not (help-build-something unit state team-units)))
        (if-not ackbot
          (build-something unit state team-units unitdefname)
          (help-build-something unit state team-units)))
      (#{"armack"} unitdefname)
      (when (or (> (rand) t2-help-build-something-chance)
                (not (help-build-something unit state team-units)))
        (if (> (rand) guard-a-lab-chance)
          (build-something unit state team-units unitdefname)
          (guard-a-lab unit state team-units)))
      (any-combat unitdefname) ; attack something
      (let [enemies (.getEnemyUnitsInRadarAndLos callback)
            unitpos (.getPos unit)
            closest-enemy (->> enemies
                               (sort-by (comp (partial u/distance unitpos) #(.getPos %)))
                               first)]
        (if closest-enemy
          (.attack unit closest-enemy (short 0) Integer/MAX_VALUE)
          (log/debug "No enemies")))
      :else
      nil)))
