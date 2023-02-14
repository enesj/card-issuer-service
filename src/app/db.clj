(ns app.db
  (:require
   [app.config :refer  [config]]
   [migratus.core :as migratus]
   [next.jdbc :as nj])
  (:import
   io.zonky.test.db.postgres.embedded.EmbeddedPostgres))

;================================Start Embedded Postgres================================================================
(def db-config
  (:db-config config))

(defn start-pg! []
  (println "Starting embedded postgres server")
  (-> (EmbeddedPostgres/builder)
      (.setPort (:port db-config))
      (.start)))

(defonce pg (start-pg!))

;==================================Create DB and datasource=============================================================

(defn make-jdbc-url [{:keys [port db-name user password]}]
  (str "jdbc:postgresql://localhost:" port "/" db-name "?user=" user "&password=" password))

(defn make-db [name]
  (let [conn (nj/get-connection (-> db-config (dissoc :db-name) make-jdbc-url))]
    (nj/execute! conn [(str "DROP DATABASE IF EXISTS " name)])
    (nj/execute! conn [(str "CREATE DATABASE " name)])))

(make-db (:db-name db-config))

(def datasource
  (nj/get-datasource (make-jdbc-url db-config)))

;====================================Execute migrations=================================================================

(defn migrate []
  (let [{:keys [migratus migrations seeds]} config
        migratus-config (assoc migratus :db {:datasource datasource})]
    (migratus/migrate (-> migratus-config
                          (merge migrations)))
    (migratus/migrate (-> migratus-config
                          (merge seeds)))))

;==================================Helper functions=====================================================================

(defn create-migration [name]
  (migratus/create (:migrations config) name))

(defn create-seed [name]
  (migratus/create (:seeds config) name))

(defn stop-pg! [pg]
  (.close pg))

;================================Function calls=========================================================================
(comment
  (migrate)
  (stop-pg! pg)
  (create-migration "add-transactions")
  (create-seed "add-user")
  (nj/execute! datasource ["select * from users"])
  (nj/execute! datasource ["insert into users(firstname,surname) values('Sean',' Corfield')"]))

