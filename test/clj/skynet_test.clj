(ns skynet-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [skynet]))


(deftest init
  (let [chimer (skynet/init nil)]
    (try
      (is (instance? java.lang.AutoCloseable chimer))
      (finally
        (.close chimer)))))
