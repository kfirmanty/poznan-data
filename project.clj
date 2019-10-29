(defproject poznan-data "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [com.google.protobuf/protobuf-java "3.10.0"]
                 [compojure "1.6.1"]
                 [yogthos/config "1.1.5"]
                 [ring "1.7.1"]
                 [org.danielsz/system "0.4.3"]
                 [clj-http "3.10.0"]
                 [cheshire "5.9.0"]
                 [ring/ring-json "0.5.0"]
                 [camel-snake-kebab "0.4.0"]]
  :main ^:skip-aot poznan-data.core
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :target-path "target/%s"
  :profiles {:uberjar {:main poznan-data.server
                       :aot [poznan-data.server]
                       :uberjar-name "poznan-data.jar"
                       :omit-source  true}}
  :jvm-opts ["-Xmx1G"])
