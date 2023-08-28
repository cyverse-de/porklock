(ns porklock.pathing
  (:require [clojure.set :as set]
            [clojure.string :as string]
            [clojure-commons.file-utils :as ft]
            [porklock.fileops :as fileops]))

(def relative-paths-to-exclude
  [".irods"
   ".irods/.irodsA"
   ".irods/.irodsEnv"
   "logs/irods-config"
   "irods.retries"
   "irods.lfretries"
   "irods-config"
   ".irodsA"
   ".irodsEnv"])

(defn- parse-exclude-file
  "Returns a list of paths read from the file at the given `exclude-file-path`, or nil if the file does not exist."
  [exclude-file-path delimiter]
  (when (ft/exists? (str exclude-file-path))
    (remove string/blank?
            (string/split (slurp exclude-file-path)
                          (re-pattern delimiter)))))

(defn- paths-to-exclude
  "Returns a list containing the static `relative-paths-to-exclude`
   and the paths read from the file at the given `exclude-file-path`."
  [exclude-file-path delimiter]
  (concat relative-paths-to-exclude
          (parse-exclude-file exclude-file-path delimiter)))

(defn exclude-files-from-dir
  "Splits up the exclude option and turns the result into paths in the source dir."
  [{source :source exclude-file :exclude delimiter :exclude-delimiter}]
  (mapv
   #(if-not (string/starts-with? % "/")
     (ft/path-join source %)
     %)
   (paths-to-exclude exclude-file delimiter)))

(defn exclude-files
  "Splits up the exclude option and turns them all into absolute paths."
  [{exclude-file :exclude delimiter :exclude-delimiter :as options}]
  (concat
   (exclude-files-from-dir options)
   (fileops/absify (paths-to-exclude exclude-file delimiter))))

(defn include-files
  "Splits up the include option and turns them all into absolute paths."
  [{includes :include delimiter :include-delimiter}]
  (if-not (string/blank? includes)
    (fileops/absify (string/split includes (re-pattern delimiter)))
    []))

(defn path-matches?
  "Determines whether or not a path matches a filter path.  If the filter path
   refers to a directory then all descendents of the directory match.
   Otherwise, only that exact path matches."
  [path filter-path]
  (if (ft/dir? filter-path)
    (string/starts-with? path filter-path)
    (= path filter-path)))

(defn should-not-exclude?
  "Determines whether or not a file should be excluded based on the list of
   excluded files."
  [excludes path]
  (not-any? #(path-matches? path %) excludes))

(defn filtered-files
  "Constructs a list of files that shouldn't be filtered out by the list of
   excluded files."
  [source-dir excludes]
  (filter #(should-not-exclude? excludes %) (fileops/files-and-dirs source-dir)))

(defn files-to-transfer
  "Constructs a list of the files that need to be transferred."
  [options]
  (let [includes (set (include-files options))
        excludes (exclude-files options)
        allfiles (set (filtered-files (:source options) excludes))]
    (println "EXCLUDING: " excludes)
    (filter #(fileops/transferable? %1) (vec (set/union allfiles includes)))))

(defn- str-contains?
  [s match]
  (if (not= (string/index-of s match) -1)
    true
    false))

(defn- fix-path
  [transfer-file sdir ddir]
  (ft/rm-last-slash
   (ft/path-join ddir (string/replace transfer-file (re-pattern sdir) ""))))

(defn relative-dest-paths
  "Constructs a sorted map between source paths and absolute destination paths
   based on the input and the given source directory."
  [transfer-files source-dir dest-dir]

  (let [sdir (ft/add-trailing-slash source-dir)]
    (apply
      merge
      (sorted-map)
      (map
        #(when (str-contains? %1 sdir)
           {%1 (fix-path %1 sdir dest-dir)})
        transfer-files))))
