(ns porklock.commands
  (:require [clj-jargon.init :as jg]
            [clj-jargon.item-info :as info]
            [clj-jargon.item-ops :as ops]
            [clj-jargon.metadata :as meta]
            [clj-jargon.permissions :as perms]
            [clojure-commons.file-utils :as ft]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [porklock.config :as cfg]
            [porklock.pathing :as pathing]
            [slingshot.slingshot :refer [throw+ try+]])
  (:import [org.irods.jargon.core.exception DuplicateDataException]
           [org.irods.jargon.core.transfer TransferStatus]))         ; needed for cursive type navigation

(def porkprint (partial println "[porklock] "))

(defn init-jargon
  [cfg-path]
  (cfg/load-config-from-file cfg-path)
  (jg/init (cfg/irods-host)
           (cfg/irods-port)
           (cfg/irods-user)
           (cfg/irods-pass)
           (cfg/irods-home)
           (cfg/irods-zone)
           (cfg/irods-resc)))

(defn retry
  "Attempt calling (func) with args a maximum of 'times' times if an error occurs.
   Adapted from a stackoverflow solution: http://stackoverflow.com/a/12068946"
  [max-attempts func & args]
  (let [result (try+
                 {:value (apply func args)}
                 (catch Object e
                   (porkprint "Error calling a function." max-attempts "attempts remaining:" e)
                   (if-not (pos? max-attempts)
                     (throw+ e)
                     {:exception e})))]
    (if-not (:exception result)
      (:value result)
      (recur (dec max-attempts) func args))))


(defn fix-meta
  [m]
  (cond
    (= (count m) 3) m
    (= (count m) 2) (conj m "default-unit")
    (= (count m) 1) (concat m ["default-value" "default-unit"])
    :else           []))

(defn avu?
  [cm path attr value]
  (filter #(= value (:value %)) (meta/get-attribute cm path attr)))


(defn- ensure-metadatum-applied
  [cm destination avu]
  (try+
    (apply meta/add-metadata cm destination avu)
    (catch DuplicateDataException _
      (porkprint "Strange." destination "already has the metadatum" (str avu ".")))))


(defn- apply-metadatum
  [cm destination avu]
  (porkprint "Might be adding metadata to" destination avu)
  (let [existent-avu (avu? cm destination (first avu) (second avu))]
    (porkprint "AVU?" destination existent-avu)
    (when (empty? existent-avu)
      (porkprint "Adding metadata" (first avu) (second avu) destination)
      (ensure-metadatum-applied cm destination avu))))


(defn apply-metadata
  [cm destination meta]
  (let [tuples (map fix-meta meta)
        dest   (ft/rm-last-slash destination)]
    (porkprint "Metadata tuples for" destination "are" tuples)
    (when (pos? (count tuples))
      (doseq [tuple tuples]
        (porkprint "Size of tuple" tuple "is" (count tuple))
        (when (= (count tuple) 3)
          (apply-metadatum cm dest tuple))))))

(defn home-folder?
  [zone full-path]
  (let [parent (ft/dirname full-path)]
    (= parent (ft/path-join "/" zone "home"))))

(defn iput-status
  "Callback function for the overallStatus function for a TransferCallbackListener."
  [^TransferStatus transfer-status]
  (let [exc (.getTransferException transfer-status)]
    (when-not (nil? exc)
      (throw exc))))

(defn- status-cb-fn
  "Returns a callback function for a TransferCallbackListener."
  [cmd]
  (fn [^TransferStatus transfer-status]
    (porkprint "-------")
    (porkprint cmd "status update:")
    (porkprint "\ttransfer state:" (.getTransferState transfer-status))
    (porkprint "\ttransfer type:" (.getTransferType transfer-status))
    (porkprint "\tsource path:" (.getSourceFileAbsolutePath transfer-status))
    (porkprint "\tdest path:" (.getTargetFileAbsolutePath transfer-status))
    (porkprint "\tfile size:" (.getTotalSize transfer-status))
    (porkprint "\tbytes transferred:" (.getBytesTransfered transfer-status))
    (porkprint "\tfiles to transfer:" (.getTotalFilesToTransfer transfer-status))
    (porkprint "\tfiles skipped:" (.getTotalFilesSkippedSoFar transfer-status))
    (porkprint "\tfiles transferred:" (.getTotalFilesTransferredSoFar transfer-status))
    (porkprint "\ttransfer host:" (.getTransferHost transfer-status))
    (porkprint "\ttransfer zone:" (.getTransferZone transfer-status))
    (porkprint "\ttransfer resource:" (.getTargetResource transfer-status))
    (porkprint "-------")
    (when-let [exc (.getTransferException transfer-status)]
      (throw exc))
    ops/continue))

(defn- force-cb-fn
  "Returns a function that can be used to indicate whether or not an operation should be forced. In our case, we always
   want the operation to be forced."
  [cmd]
  (fn [abs-path collection?]
    (let [object-type (if collection? "collection" "data object")]
      (porkprint "force" cmd "of" object-type (str abs-path ".")))
    ops/yes-for-all))

(defn get-tcl
  "Returns a TransferCallbackListener for the given command."
  [cmd]
  (ops/transfer-callback-listener iput-status (status-cb-fn cmd) (force-cb-fn cmd)))

(defn- parent-exists?
  "Returns true if the parent directory exists or is /iplant/home"
  [cm dest-dir]
  (if (home-folder? (:zone cm) dest-dir)
    true
    (info/exists? cm (ft/dirname dest-dir))))

(defn- parent-writeable?
  "Returns true if the parent directorty is writeable or is /iplant/home."
  [cm user dest-dir]
  (if (home-folder? (:zone cm) dest-dir)
    true
    (perms/is-writeable? cm user (ft/dirname dest-dir))))

(defn- relative-destination-paths
  [options]
  (pathing/relative-dest-paths (pathing/files-to-transfer options)
                       (ft/abs-path (:source options))
                       (:destination options)))

(def error? (atom false))

(defn- upload-files
  [admin-cm cm options]
  (doseq [[src dest] (seq (relative-destination-paths options))]
    (let [dir-dest (ft/dirname dest)]
      (if-not (or (.isFile (io/file src))
                  (.isDirectory (io/file src)))
        (porkprint "Path" src "is neither a file nor a directory.")
        (do
          ;;; It's possible that the destination directory doesn't
          ;;; exist yet in iRODS, so create it if it's not there.
          (porkprint "Creating all directories in iRODS down to" dir-dest)
          (when-not (info/exists? cm dir-dest)
            (ops/mkdirs cm dir-dest))

          ;;; The destination directory needs to be tagged with AVUs
          ;;; for the App and Execution.
          (porkprint "Applying metadata to" dir-dest)
          (apply-metadata admin-cm dir-dest (:meta options))

          (try+
           (if (ft/dir? src)
             (when-not (info/exists? cm dest)
              (ops/mkdir cm dest))
             (retry 10 ops/iput cm src dest (get-tcl "iput")))

            ;;; Apply the App and Execution metadata to the newly uploaded file/directory.
            (porkprint "Applying metadata to" dest)
            (apply-metadata admin-cm dest (:meta options))
            (catch Object err
              (porkprint "iput failed:" err)
              (reset! error? true))))))))

(def script-loc
  (memoize (fn []
             (ft/dirname (ft/abs-path (System/getenv "SCRIPT_LOCATION"))))))

(defn- upload-nfs-files
  [admin-cm cm options]
  (when (and (System/getenv "SCRIPT_LOCATION") (not (:skip-parent-meta options)))
    (let [dest       (ft/path-join (:destination options) "logs")
          exclusions (set (pathing/exclude-files-from-dir (merge options {:source (script-loc)})))]
      (porkprint "Exclusions:\n" exclusions)
      (doseq [^java.io.File fileobj (file-seq (clojure.java.io/file (script-loc)))]
        (let [src       (.getAbsolutePath fileobj)
              dest-path (ft/path-join dest (ft/basename src))]
          (try+
           (when-not (or (.isDirectory fileobj) (contains? exclusions src))
             (retry 10 ops/iput cm src dest (get-tcl "iput"))
             (apply-metadata admin-cm dest-path (:meta options)))
           (catch [:error_code "ERR_BAD_EXIT_CODE"] err
             (porkprint "Command exited with a non-zero status:" err)
             (reset! error? true))))))))

(defn iput-command
  "Runs the iput icommand, tranferring files from the --source
   to the remote --destination."
  [options]
  (jg/with-jargon (init-jargon (:config options)) [admin-cm]
    (jg/with-jargon (init-jargon (:config options)) :client-user (:user options) [cm]
      ;;; The parent directory needs to actually exist, otherwise the dest-dir
      ;;; doesn't exist and we can't safely recurse up the tree to create the
      ;;; missing directories. Can't even check the perms safely if it doesn't
      ;;; exist.
      (when-not (parent-exists? cm (:destination options))
        (porkprint (ft/dirname (:destination options)) "does not exist.")
        (System/exit 1))

      ;;; Need to make sure the parent directory is writable just in
      ;;; case we end up having to create the destination directory under it.
      (when-not (parent-writeable? cm (:user options) (:destination options))
        (porkprint (ft/dirname (:destination options)) "is not writeable.")
        (System/exit 1))

      ;;; Now we can make sure the actual dest-dir is set up correctly.
      (when-not (info/exists? cm (:destination options))
        (porkprint "Path" (:destination options) "does not exist. Creating it.")
        (ops/mkdir cm (:destination options)))

      (upload-files admin-cm cm options)

      (when-not (:skip-parent-meta options)
        (porkprint "Applying metadata to" (:destination options))
        (apply-metadata admin-cm (:destination options) (:meta options))
        (doseq [^java.io.File fileobj (file-seq (info/file cm (:destination options)))]
          (apply-metadata admin-cm (.getAbsolutePath fileobj) (:meta options))))

      ;;; Transfer files from the NFS mount point into the logs
      ;;; directory of the destination
      (upload-nfs-files admin-cm cm options)

      (when @error?
        (throw (Exception. "An error occurred tranferring files into iRODS. Please check the above logs for more information."))))))

(defn- parse-source-list
  "Returns a list of paths read from the the given `source-list` path list file, or nil if the file does not exist.
   Removes lines beginning with a hash character: #."
  [source-list]
  (when (ft/exists? (str source-list))
    (->> source-list
         slurp
         string/split-lines
         (remove #(string/starts-with? % "#")))))

(defn apply-input-metadata
  [cm user fpath meta]
  (if-not (info/is-dir? cm fpath)
    (when (perms/owns? cm user fpath)
      (apply-metadata cm fpath meta))
    (doseq [^java.io.File f (file-seq (info/file cm fpath))]
      (let [abs-path (.getAbsolutePath f)]
        (when (perms/owns? cm user abs-path)
          (apply-metadata cm abs-path meta))))))

(defn iget-command
  "Runs the iget icommand, retrieving files from --source and --source-list to the local --destination."
  [{:keys [config user meta source-list source destination]}]
  (jg/with-jargon (init-jargon config) [admin-cm]
    (jg/with-jargon (init-jargon config) :client-user user [cm]
      (let [paths (remove string/blank? (conj (parse-source-list source-list) source))]
        (doseq [remote-path paths]
          (apply-input-metadata admin-cm user (ft/rm-last-slash remote-path) meta)
          (retry 10 ops/iget cm remote-path destination (get-tcl "iget")))))))
