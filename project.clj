(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.cyverse/porklock "2.8.1-SNAPSHOT"
  :description "A command-line tool for interacting with iRODS."
  :url "https://github.com/cyverse-de/porklock"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :main ^:skip-aot porklock.core
  :profiles {:uberjar {:aot :all}}
  :uberjar-name "porklock-standalone.jar"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [commons-io/commons-io "2.4"]
                 [slingshot "0.12.2"]
                 [org.cyverse/clj-jargon "2.8.0"]
                 [org.cyverse/clojure-commons "2.8.0"]
                 [org.cyverse/common-cli "2.8.0"]])
