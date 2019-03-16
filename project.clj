(defproject goodreads "0.1.0-SNAPSHOT"
  :description "Books recommendation tool"
  :license {:name "Proprietary"}
  :jvm-opts ^:replace ["-server"
                       "-XX:+UseConcMarkSweepGC"
                       "-Xmx256m"
                       "-XX:-OmitStackTraceInFastThrow"]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/data.xml "0.2.0-alpha6"]
                 [org.clojure/data.zip "0.1.2"]
                 [aleph "0.4.4"]
                 [org.clojure/test.check "0.10.0-alpha4"]]
  :main ^:skip-aot goodreads.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :source-paths ["src"]
  :test-paths ["test"]
  :dev {:resource-paths ["test/resources"]})
