(ns porklock.validation
  (:require [clojure.pprint :refer [pprint]]
            [clojure-commons.error-codes :as ce]
            [clojure-commons.file-utils :as ft]
            [porklock.pathing :as pathing]
            [slingshot.slingshot :refer [throw+]]))

(def ERR_MISSING_OPTION "ERR_MISSING_OPTION")
(def ERR_PATH_NOT_ABSOLUTE "ERR_PATH_NOT_ABSOLUTE")
(def ERR_ACCESS_DENIED "ERR_ACCESS_DENIED")

(defn usable?
  [_]
  true)

(defn validate-put
  "Validates information for a put operation.
   Throws an error if the input is invalid.

   For a put op, all of the local files must exist,
   all of the --include files must exist, all
   of the .irods/* files must exist, and the paths
   to the executable must exist."
  [options]
  (when-not (:user options)
    (throw+ {:error_code ERR_MISSING_OPTION
             :option "--user"}))

  (when-not (usable? (:user options))
    (throw+ {:error_code ERR_ACCESS_DENIED}))

  (when-not (:source options)
    (throw+ {:error_code ERR_MISSING_OPTION
             :option "--source"}))

  (when-not (:destination options)
    (throw+ {:error_code ERR_MISSING_OPTION
             :option "--destination"}))

  (when-not (ft/dir? (:source options))
      (throw+ {:error_code ce/ERR_NOT_A_FOLDER
               :path (:source options)}))

  (when-not (ft/abs-path? (:destination options))
    (throw+ {:error_code ERR_PATH_NOT_ABSOLUTE
             :path (:destination options)}))

  (when-not (:config options)
    (throw+ {:error_code ERR_MISSING_OPTION
             :option "--config"}))

  (println "Files to upload: ")
    (pprint (pathing/files-to-transfer options))
    (println " ")

  (let [paths-to-check (flatten [(pathing/files-to-transfer options)
                                 (:config options)])]

    (println "Paths to check: ")
    (pprint paths-to-check)
    (doseq [p paths-to-check]
      (when-not (ft/exists? p)
        (throw+ {:error_code ce/ERR_DOES_NOT_EXIST
                 :path p})))))

(defn validate-get
  "Validates info for a get op. Throws an error
   on invalid input.

   For a get op, the following files must exist.
     * Path to 'iget'.
     * Destination directory.
     * .irods/.irodsA and .irods/.irodsEnv files.
   Additionally:
     * Destination must be a directory."
  [options]
  (when-not (:user options)
    (throw+ {:error_code ERR_MISSING_OPTION
             :option "--user"}))

  (when-not (usable? (:user options))
    (throw+ {:error_code ERR_ACCESS_DENIED}))

  (when-not (or (:source options) (:source-list options))
    (throw+ {:error_code ERR_MISSING_OPTION
             :option "--source or --source-list"}))

  (when-not (:destination options)
    (throw+ {:error_code ERR_MISSING_OPTION
             :option "--destination"}))

  (when-not (:config options)
    (throw+ {:error_code ERR_MISSING_OPTION
             :option "--config"}))

  (let [paths-to-check (flatten [(:destination options)
                                 (:config options)])]
    (doseq [p paths-to-check]
      (when-not (ft/exists? p)
        (throw+ {:error_code ce/ERR_DOES_NOT_EXIST
                 :path p})))

    (when-not (ft/dir? (:destination options))
      (throw+ {:error_code ce/ERR_NOT_A_FOLDER
               :path (:destination options)}))))
