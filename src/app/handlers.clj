(ns app.handlers
  (:require
   [app.domain.users :as users]
   [app.domain.file-notifications :refer [send-notification]]
   [clojure.core.async :refer [thread]]
   [app.domain.transactions :refer [deposit withdraw transfer broke-connection-in-transaction
                                    get-all-transactions get-all-balances get-balance]]
   [clojure.data.json :as json]))

; Helper to get the parameter from :path-params in req
(defn get-parameter [req p-name] (get (:path-params req) p-name))

; Home Page
;====================================================================================================

(defn home-page [_]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "Card issuer authorizations service"})

; Users
;====================================================================================================

(defn users
  "Returns all users"
  [_]
  {:status 200
   :headers {"Content-Type" "text/json"}
   :body  (json/write-str (users/get-all-users))})

(defn add-user
  "Adds a new user"
  [req]
  {:status 200
   :headers {"Content-Type" "text/json"}
   :body (-> (let [p (partial get-parameter req)]
               (json/write-str (first (users/add-user (p :firstname) (p :surname) (p :email))))))})

; Transactions
;====================================================================================================

(defn transactions
  "Returns all transactions"
  [_]
  {:status 200
   :headers {"Content-Type" "text/json"}
   :body (json/write-str (get-all-transactions))})

(defn add-transaction
  "Call corresponding function based on type argument.
   All functions returns nil if successful and an error message if not"
  [req]
  {:status 200
   :headers {"Content-Type" "text/json"}
   :body (-> (let [p (partial get-parameter req)
                   data (:path-params req)
                   result (case (p :type)
                            "deposit" (deposit (p :amount) (p :currency-code) (p :user-email))
                            "withdrawal" (withdraw (p :amount) (p :currency-code) (p :user-email))
                            "transfer" (transfer (p :amount) (p :currency-code) (p :user-email) (p :receiver-email))
                            "broken" (broke-connection-in-transaction (p :amount) (p :currency-code) (p :user-email)))
                   body (if result {:is-authorised false
                                    :description result}
                                   {:is-authorised true})]
               (thread (send-notification (merge data body)))
               (json/write-str body)))})



; Balances
;====================================================================================================

(defn balances
  "Returns all balances"
  [_]
  {:status 200
   :headers {"Content-Type" "text/json"}
   :body  (json/write-str (get-all-balances))})

(defn balance
  "Returns the balance for a user and currency"
  [req]
  {:status 200
   :headers {"Content-Type" "text/json"}
   :body (-> (let [p (partial get-parameter req)]
               (json/write-str (get-balance (p :user-email) (p :currency-code)))))})
