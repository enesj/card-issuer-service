(ns app.config
  (:require
   [clojure.java.io :as io]))

(defn deep-merge
  "Same as clojure.core/merge, except that
  it recursively applies itself to every nested map."
  [& maps]
  (apply merge-with
         (fn [& args]
           (if (every? map? args)
             (apply deep-merge args)
             (last args)))
         maps))

(def common-config
  (-> (io/resource "common.edn")
      slurp
      (read-string)))

(def config
  (let [profile-db-conf (->
                         (io/resource "config.edn")
                         slurp
                         (read-string))]
    (deep-merge common-config profile-db-conf)))
