(set-env!
 :dependencies  '[[org.clojure/clojure "1.8.0"]
                  [adzerk/bootlaces "0.1.13" :scope "test"] ;; TODO: what does :scope test mean?
                  [com.novemberain/pantomime "2.8.0"]
                  [org.clojars.hozumi/clj-commons-exec "1.2.0"]]
 :source-paths    #{"tests"}
 :resource-paths  #{"src" "resources"})

(require
 '[adzerk.bootlaces :refer :all])

(def +version+ "0.0.1")

(bootlaces! +version+)

(task-options!
 aot {:all true} ;; TODO: what does aot mean?
 pom  {:project        'org.clojars.basset/boot-graphicsmagick
       :version        +version+
       :description    "Boot task for transforming graphicss with Graphicsmagick"
       :url            "https://github.com/coreygrunewald/boot-graphicsmagick"
       :scm            {:url "https://github.com/coreygrunewald/boot-graphicsmagick"}
       :license        {"Eclipse Public License"
                        "http://www.eclipse.org/legal/epl-v10.html"}})

(deftask install-locally
  "Install locally"
  []
  (comp (pom) (jar) (install)))

(deftask release-snapshot
  "Release snapshot"
  []
  (comp (pom) (jar) (push-snapshot)))
