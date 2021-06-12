(ns skynet.core-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [skynet.core :as skynet])
  (:import
    (com.springrts.ai AICallback)))


(def test-logfile "fake.log")


(deftype MockAI
  [state])


(deftest ai-init
  (let [logs (atom [])
        callback (reify AICallback
                   (DataDirs_getConfigDir [this]
                     ".")
                   (DataDirs_getWriteableDir [this]
                     ".")
                   (DataDirs_allocatePath [this path _ _ _ _]
                     test-logfile)
                   (Log_log [this message]
                     (swap! logs conj message))
                   (SkirmishAI_getTeamId [this]
                     3)
                   (SkirmishAI_Info_getSize [this]
                     2)
                   (SkirmishAI_Info_getKey [this id]
                     (get ["i1" "i1"] id))
                   (SkirmishAI_Info_getValue [this id]
                     (get ["iv1" "iv2"] id))
                   (SkirmishAI_OptionValues_getSize [this]
                     2)
                   (SkirmishAI_OptionValues_getKey [this id]
                     (get ["o1" "o2"] id))
                   (SkirmishAI_OptionValues_getValue [this id]
                     (get ["ov1" "ov2"] id)))
        this (MockAI. (atom {}))]
    (is (= 0
           (skynet/ai-init this 2 callback)))
    (is (= ["init" (str "logging to " test-logfile)]
           @logs))
    (is (= {:info {"i1" "iv2"}
            :options {"o1" "ov1"
                      "o2" "ov2"}}
           (select-keys @(.state this) [:info :options])))))
