(require '[clojure.java.shell :refer (sh)])
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
  :plugins [[jonase/eastwood "1.4.3"]
            [lein-ancient "1.0.0"]
            [test2junit "1.4.4"]]
  ;; Fail the build on a new dependency conflict rather than printing a
  ;; warning nobody reads.
  :pedantic? :abort
  ;; Records versions Leiningen already resolves, read off the resolved
  ;; classpath rather than copied from lein's "Consider using these
  ;; :managed-dependencies" hint -- that hint names the version that LOST the
  ;; conflict, so pasting it would be a silent upgrade.
  ;;
  ;; The jackson-* entries hold the coherent 2.14.1 family that jargon-core
  ;; 4.3.7.0-RELEASE brings; it is pinned :upgrade false for iRODS.
  :managed-dependencies [[cheshire "5.13.0"]
                         [com.fasterxml.jackson.core/jackson-annotations "2.14.1"]
                         [com.fasterxml.jackson.core/jackson-core "2.14.1"]
                         [com.fasterxml.jackson.core/jackson-databind "2.14.1"]
                         [com.fasterxml.jackson.dataformat/jackson-dataformat-cbor "2.14.1"]
                         [com.fasterxml.jackson.dataformat/jackson-dataformat-smile "2.14.1"]
                         [commons-codec "1.15"]
                         [prismatic/schema "1.1.12"]
                         [ring/ring-codec "1.1.0"]
                         [ring/ring-core "1.6.3"]]
  :dependencies [[org.clojure/clojure "1.12.5"]
                 [org.clojure/tools.cli "1.4.256"]
                 [org.clojure/tools.logging "1.3.1"]
                 [commons-io/commons-io "2.22.0"]
                 [slingshot "0.12.2"]
                 [org.cyverse/clj-jargon "3.1.6"]
                 [org.cyverse/clojure-commons "3.0.13"]
                 [org.cyverse/common-cli "2.8.3"]])
