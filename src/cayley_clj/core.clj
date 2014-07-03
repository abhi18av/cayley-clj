(ns cayley-clj.core
  (:require [clojure.string :as s]
            [clojure.data.json :as json]
            [cayley-clj.http :as h]))

(defn- shape-args
  [args]
  (if (coll? args)
    (map (fn [a]
           (if (integer? a)
             a
             (format "\"%s\"" a))) args)
    (if (integer? args)
      args
      (format "\"%s\"" args))))

(defn- shape-verbs
  [[fname & args]]
  (if (keyword? fname)
    (if args
      (if-not (vector? (first args))
        (if (contains? #{:Follow :FollowR :Intersect :Union} fname)
          (format "%s(%s)" (name fname) (first args))
          (format "%s(%s)" (name fname) (shape-args (first args))))
        (if (keyword? (ffirst args))
          (format "%s(%s)" (name fname) (shape-verbs (first args)))
          (format "%s(%s)" (name fname) (s/join "," (shape-args (first args))))))
      (format "%s()" (name fname)))
    (format "%s" fname)))

(defn- to-gremlin
  [q]
  (s/join "." (map shape-verbs q)))

(defn- build-path
  [[varname path]]
  (format "var %s = %s" varname (to-gremlin path)))

(defn query
  [q url & paths]
  (if-let [gremlin-paths (map build-path paths)]
    (let [paths-and-query (s/join "\r\n" (reverse (conj gremlin-paths (to-gremlin q))))]
      (h/send paths-and-query url))
    (h/send (to-gremlin q) url)))

(defn- shape-triple-quad
  [triple-quad]
  (let [triple-keys [:subject :predicate :object :provenance]]
    (zipmap triple-keys triple-quad)))

(defn write
  [triples-quads url]
  (h/send (json/write-str (map shape-triple-quad triples-quads)) url))
