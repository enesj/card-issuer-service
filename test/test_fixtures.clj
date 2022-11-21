(ns test-fixtures
  (:require
   [app.core :refer [-main]]
   [app.db :refer [datasource]]
   [next.jdbc :as nj]
   [app.domain.file-notifications :refer [delete-notifications-files]]
   [ring.adapter.jetty9 :as jetty]))

(defn start-app-clear-db
  "Fixture for starting the app with webserver and truncating PG database between tests"
  [test]
  (let [webserver (-main)]
    (try
      (delete-notifications-files)
      (test)
      (finally
        (jetty/stop-server webserver)
        (nj/execute! datasource
                     ["DO $$ BEGIN
           EXECUTE 'TRUNCATE TABLE '
           || (SELECT string_agg((table_schema::text) || '.' || (table_name::text), ',')
               FROM information_schema.tables
               WHERE (table_schema = 'public')
                   AND table_type = 'BASE TABLE'
                   AND table_name != 'migrations')
           || ' CASCADE';
         END; $$;"])))))
