(defproject skynet "0.1"
  :source-paths ["src/clj"]
  :test-paths ["test/clj"]
  :java-source-paths ["src/java"]
  :dependencies [[aleph "0.4.6"]
                 [clojure.java-time "0.3.2"]
                 [com.taoensso/sente "1.15.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.springrts/ai-interface-java "0.1"]
                 [com.springrts/ai-interface-javaoo "0.1"]
                 [compojure "1.6.2"]
                 [jarohen/chime "0.3.2"]
                 [javax.vecmath/vecmath "1.5.2"]
                 [net.mikera/core.matrix "0.62.0"]
                 [org.clojure/clojure "1.10.2-alpha1"]
                 [org.clojure/core.async "1.3.610"]
                 [org.clojure/spec.alpha "0.2.187"]
                 [ring/ring-anti-forgery "1.3.0"]
                 [ring/ring-core "1.8.1"]
                 [rm-hull/clustering "0.2.0"]]
  :plugins [[lein-localrepo "0.5.4"]
            [com.jakemccrary/lein-test-refresh "0.24.1"]]
  :test-refresh {:changes-only true
                 :quiet false}
  :aot :all
  :uberjar-name "SkirmishAI.jar"
  :repositories {"local" "file:repo"}
  :prep-tasks ["compile" "javac"]) ; Clojure before Java
