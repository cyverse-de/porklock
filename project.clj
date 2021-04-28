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
  :plugins [[test2junit "1.2.2"]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/tools.logging "0.3.1"]
                 [commons-io/commons-io "2.5"]
                 [slingshot "0.12.2"]
                 [org.cyverse/clj-jargon "3.0.0-SNAPSHOT"]
                 [org.cyverse/clojure-commons "2.8.3"]
                 [org.cyverse/common-cli "2.8.1"]]
  :repositories [["cyverse-de"
                  {:url "https://raw.github.com/cyverse-de/mvn/master/releases"}]
                 ["dice.repository"
                  {:url "https://raw.github.com/DICE-UNC/DICE-Maven/master/releases"}]
                 ["renci-snapshot.repository"
                  {:url "https://ci-dev.renci.org/nexus/content/repositories/renci-snapshot/"}]])
