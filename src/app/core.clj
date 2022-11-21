(ns app.core
  (:require
   [app.db :as db]
   [app.handlers :as handlers]
   [app.logging :as logging]
   [reitit.dev.pretty :as pretty]
   [reitit.ring :as ring]
   [ring.adapter.jetty9 :as jetty])
  (:gen-class))

(def app
  (ring/ring-handler
   (ring/router
    [["/" {:get handlers/home-page}]
     ["/users"
      ["/all" handlers/users-handler]
      ["/add/:firstname/:surname/:email" handlers/add-user-handler]]
     ["/authorizations"
      ["/:amount/:currency-code/:user-email/:type" handlers/add-transaction-handler]
      ["/:amount/:currency-code/:user-email/:type/:receiver-email" handlers/add-transaction-handler]]
     ["/transactions" handlers/transactions-handler]
     ["/balances"
      ["/all" handlers/balances-handler]
      ["/:user-email/:currency-code" handlers/balance-handler]]]
    {:exception pretty/exception})
   (constantly {:status 404, :body ""})))

(defn -main
  "Main entry point"
  [& _]
  (let [port 3000]
    (logging/init!)
    (db/migrate)
    (jetty/run-jetty #'app {:port port :join? false})))



