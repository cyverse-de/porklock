(ns porklock.fileops
  (:use [clojure.java.io :only (file)])
  (:require [clojure-commons.file-utils :as ft])
  (:import [org.apache.commons.io FileUtils]
           [org.apache.commons.io.filefilter TrueFileFilter DirectoryFileFilter]))

(defn files-and-dirs
  "Returns a recursively listing of all files and subdirectories
   present under 'parent'."
  [parent]
  (map
    #(ft/normalize-path (.getAbsolutePath %))
    (FileUtils/listFilesAndDirs
      (file parent)
      TrueFileFilter/INSTANCE
      DirectoryFileFilter/INSTANCE)))

(defn absify
  "Takes in a sequence of paths and turns them all into absolute paths."
  [paths]
  (map ft/abs-path paths))

(defn transferable?
  [path]
  (or (.isFile (file path)) (.isDirectory (file path))))
