(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.cyverse/porklock "2.12.0-SNAPSHOT"
  :description "A command-line tool for interacting with iRODS."
  :url "https://github.com/cyverse-de/porklock"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :main ^:skip-aot porklock.core
  :profiles {:uberjar {:aot :all}}
  :uberjar-name "porklock-standalone.jar"
  :plugins [[jonase/eastwood "1.4.0"]
            [lein-ancient "0.7.0"]
            [test2junit "1.2.2"]]
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/tools.cli "1.0.219"]
                 [org.clojure/tools.logging "1.2.4"]
                 [commons-io/commons-io "2.14.0"]
                 [slingshot "0.12.2"]
                 [org.cyverse/clj-jargon "3.1.0"]
                 [org.cyverse/clojure-commons "3.0.7"]
                 [org.cyverse/common-cli "2.8.1"]
                 [org.irods.jargon/jargon-core "4.1.10.0-RELEASE"
                  :exclusions [[org.jglobus/JGlobus-Core]
                               [org.slf4j/slf4j-api]
                               [org.slf4j/slf4j-log4j12]]]
                 [org.irods.jargon/jargon-data-utils "4.1.10.0-RELEASE"
                  :exclusions [[org.slf4j/slf4j-api]
                               [org.slf4j/slf4j-log4j12]]]
                 [org.irods.jargon/jargon-ticket "4.1.10.0-RELEASE"
                  :exclusions [[org.slf4j/slf4j-api]
                               [org.slf4j/slf4j-log4j12]]]]
  :repositories [["cyverse-de"
                  {:url "https://raw.github.com/cyverse-de/mvn/master/releases"}]
                 ["dice.repository"
                  {:url "https://raw.github.com/DICE-UNC/DICE-Maven/master/releases"}]
                 ["renci-snapshot.repository"
                  {:url "https://ci-dev.renci.org/nexus/content/repositories/renci-snapshot/"}]])
