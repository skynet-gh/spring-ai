(ns user
  (:require
    [aleph.http :as http]
    [clojure.java.io :as io]
    [clojure.tools.namespace.repl :refer [refresh set-refresh-dirs]]
    [hawk.core :as hawk]
    [pjstadig.humane-test-output]
    [skynet.core]
    [skynet.core-test]
    [skynet.server]))


(pjstadig.humane-test-output/activate!)

(set-refresh-dirs "src/clj" "test/clj")

(refresh)


(defn refresh-on-file-change [context event]
  (when-let [file (:file event)]
    (let [f (io/file file)]
      (when (and (.exists f) (not (.isDirectory f)))
        (future (refresh)))))
  context)


(hawk/watch! [{:paths ["src" "test"]
               :handler refresh-on-file-change}])


(defn start []
  (http/start-server skynet.server/my-app {:port 8080}))
