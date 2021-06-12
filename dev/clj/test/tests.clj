(ns tests
  (:require
    [com.jakemccrary.test-refresh :refer [monitor-project]]
    [pjstadig.humane-test-output]))


(defn -main []
  (pjstadig.humane-test-output/activate!)
  (monitor-project
    ["test/clj"]
    {:changes-only true
     :do-not-monitor-keystrokes false
     :nses-and-selectors [:ignore [[(constantly true)]]]
     :with-repl false}))
