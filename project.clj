(defproject skynet "0.1"
  :source-paths ["src/clj"]
  :test-paths ["test/clj"]
  :java-source-paths ["src/java"]
  :dependencies [[com.taoensso/timbre "4.10.0"]
                 [com.springrts/ai-interface-java "0.1"]]
  :plugins [[lein-localrepo "0.5.4"]]
  :aot :all
  :uberjar-name "SkirmishAI.jar"
  :repositories {"local" "file:repo"}
  :prep-tasks ["compile" "javac"]) ; Clojure before Java
