(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/porklock "4.2.4"
  :description "A command-line tool for interacting with iRODS."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :main ^:skip-aot porklock.core
  :profiles {:uberjar {:aot :all}}
  :uberjar-name "porklock-standalone.jar"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [cheshire "5.4.0"]
                 [com.cemerick/url "0.1.1"]
                 [com.novemberain/welle "3.0.0"]
                 [commons-io/commons-io "2.4"]
                 [slingshot "0.12.2"]
                 [org.iplantc/clj-jargon "4.2.4"]
                 [org.iplantc/clojure-commons "4.2.4"]]
  :plugins [[org.iplantc/lein-iplant-rpm "4.2.4"]]
  :iplant-rpm {:summary "Porklock"
               :type :command
               :exe-files ["curl_wrapper.pl"]}
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
