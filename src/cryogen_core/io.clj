(ns cryogen-core.io
  (:require [clojure.java.io :refer [file]]
            [me.raynes.fs :as fs]
            [clojure.java.io :as io]))

(def public "resources/public")

(defn get-resource [resource]
  (-> (Thread/currentThread)
      (.getContextClassLoader)
      (.getResource resource)
      (.toURI)
      (file)))

(defn find-assets [f ext]
  (->> (get-resource f)
       file-seq
       (filter (fn [file] (-> file .getName (.endsWith ext))))))

(defn create-folder [folder]
  (let [loc (file (str public folder))]
    (when-not (.exists loc)
      (.mkdirs loc))))

(defn wipe-public-folder [keep-files]
  (let [filenamefilter (reify java.io.FilenameFilter (accept [this _ filename] (not (some #{filename} keep-files))))]
    (doseq [path (.listFiles (file public) filenamefilter)]
      (fs/delete-dir path))))

(defn copy-images-from-markdown-folders [{:keys [blog-prefix]}]
  (let [blog-prefix-relative (if (= \/ (first blog-prefix)) (subs blog-prefix 1) blog-prefix)]
    (doseq [asset (fs/find-files "resources/templates" #".+(jpg|jpeg|png|gif)")]
      (fs/copy asset (io/file public blog-prefix-relative "img" (.getName asset))))))

(defn copy-resources [{:keys [blog-prefix resources]}]  
  (doseq [resource resources]
    (let [src (str "resources/templates/" resource)
          target (str public blog-prefix "/" resource)]      
      (cond
        (not (.exists (file src)))
        (throw (IllegalArgumentException. (str "resource " src " not found")))
        (.isDirectory (file src))
        (fs/copy-dir src target)
        :else
        (fs/copy src target)))))