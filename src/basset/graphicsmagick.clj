(ns basset.graphicsmagick
  {:boot/export-tasks true}
  (:require [boot.core :as boot :refer [deftask]]
            [boot.util :as util]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clj-commons-exec :as exec]
            [pantomime.mime  :as pm]))

(defn- commit [fileset tmp]
  (-> fileset
      (boot/add-resource tmp)
      boot/commit!))

(defn- ^String extension [name]
  (last (seq (string/split name #"\."))))

(defn- create-filepath
  "Creates a filepath using system path separator."
  [& args]
  (.getPath (apply io/file args)))

(defn- parent-path [filepath filename-with-extension]
  (if (.endsWith filepath filename-with-extension)
    (.substring filepath 0 (- (count filepath)
                              (count filename-with-extension)))
    filepath))

(defn- add-filedata [f]
  (let [tmpfile   (boot/tmp-file f)
        full-path (.getPath tmpfile)
        filename  (.getName tmpfile)
        tmp-path  (boot/tmp-path f)
        io-file   (io/file full-path)
        mime-type (pm/mime-type-of io-file)
        file-type (first (string/split mime-type #"/"))]
    {:filename       filename
     :path           tmp-path
     :mime-type      mime-type
     :file-type      file-type
     :full-path      full-path
     :parent-path    (parent-path tmp-path filename)
     :extension      (extension filename)}))

(def ^:private +graphicsmagick-defaults+
  {:extensions #{"jpeg" "jpg" "png"}
   :patterns #{#"\.*"}
   :gm "/usr/local/bin/gm"})

;; TODO: do we specify a target folder so we don't overwrite the original images???
;; TODO: what is the best practice for writing to a target?
(deftask graphicsmagick
  "Process images in your resource path using Graphicsmagick."
  [e extensions EXTENSIONS #{str} "Image extensions to be selected."
   p patterns   PATTERNS   #{regex} "Patterns to match filenames."
   c command    COMMAND    str "The graphicsmagick command to execute."
   a args       ARGS       str  "The arguments for the graphickmagick command."
   ;; o output-dir OUTPUTDIR  str "The directory to place processed images." ;; TODO: i don't think we need this!
   ;; unless we want to target putting all the images in a specific directory!
   ;; the target directory should take care of output!
   ;; TODO: we should offer a filename transform e.g. basset.png => basset@2x.png
   g gm         EXECUTABLE str "The location of the graphicsmagick executable."]
  (let [options (merge +graphicsmagick-defaults+ *opts*)
        gm-exec (io/as-file (:gm options))
        tmp (boot/tmp-dir!)
        tmp-path (.getPath tmp)]
    (cond (not (:command options))
          (util/fail "The -c/--command option is required!")
          (not (:args options))
          (util/fail "The -a/--args option is required!")
          (not (.exists gm-exec))
          (util/fail "No graphicsmagick executable found!")
          :else
          (boot/with-pre-wrap fileset
            (let [image-files (->>
                               fileset
                               boot/input-files
                               (boot/by-ext (:extensions options))
                               (boot/by-re (:patterns options))
                               (map add-filedata))]
              (if (empty? image-files)
                (do (util/info "No images found to process.") fileset)
                (doseq [image-file image-files]
                  (let [image-file-path (:full-path image-file)
                        target-file-directory (create-filepath tmp-path (:parent-path image-file))
                        target-directory-created (.mkdirs (java.io.File. target-file-directory))
                        target-file-path (create-filepath tmp-path (:path image-file))
                        cmd-vec (vec
                                 (concat [(.getPath gm-exec)
                                          (:command options)
                                          (:full-path image-file)]
                                         (vec (string/split (:args options) #" "))
                                         [target-file-path]))
                        cmd-result @(exec/sh cmd-vec {:dir tmp-path})
                        exitcode    (:exit cmd-result)
                        errormsg    (:err cmd-result)]
                    (assert
                     (= 0 exitcode)
                     (util/info (string/join "\n" ["Graphicsmagick failed:"
                                                   errormsg
                                                   "Command:"
                                                   (string/join " " cmd-vec)])))
                    )))
                  (commit fileset tmp))))))
