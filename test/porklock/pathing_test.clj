(ns porklock.pathing-test
  (:use [clojure.test])
  (:require [porklock.pathing :as pathing]))

(def exclusions-file-path "test/resources/exclusions.list")

(deftest exclude-files-from-dir-test
  (testing "exclude-files-from-dir"
    (let [exclude-paths (set (pathing/exclude-files-from-dir {:exclude exclusions-file-path :exclude-delimiter "\n"}))]
      (is (contains? exclude-paths "test1.txt"))
      (is (contains? exclude-paths "test2.txt"))
      (is (contains? exclude-paths "test3.txt")))))

(deftest exclude-files-test
  (testing "exclude-files"
   (let [exclude-paths (set (pathing/exclude-files {:exclude exclusions-file-path :exclude-delimiter "\n"}))]
     (is (contains? exclude-paths "test1.txt"))
     (is (contains? exclude-paths "test2.txt"))
     (is (contains? exclude-paths "test3.txt")))))
