(ns app.handlers
  (:require
   [app.domain.users :refer [add-user get-all-users]]
   [app.domain.file-notifications :refer [send-notification]]
   [clojure.core.async :refer [go]]
   [app.domain.transactions :refer [deposit withdraw transfer get-all-transactions get-all-balances get-balance]]
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

(defn users-handler
  "Returns all users"
  [_]
  {:status 200
   :headers {"Content-Type" "text/json"}
   :body  (json/write-str (get-all-users))})

(defn add-user-handler
  "Adds a new user"
  [req]
  {:status 200
   :headers {"Content-Type" "text/json"}
   :body (-> (let [p (partial get-parameter req)]
               (json/write-str (first (add-user (p :firstname) (p :surname) (p :email))))))})

; Transactions
;====================================================================================================

(defn transactions-handler
  "Returns all transactions"
  [_]
  {:status 200
   :headers {"Content-Type" "text/json"}
   :body (json/write-str (get-all-transactions))})

(defn response-body
  "Returns a response body and sends a notification.
   The notification is sent in a separate thread to prevent delays in response."
  [result data]
  (if result
    (let [body {:is-authorised false
                :description result}]
      (go (send-notification (merge data body)))
      body)
    (let [body {:is-authorised true}]
      (go (send-notification (merge data body)))
      body)))

(defn add-transaction-handler
  "Call corresponding function based on type argument.
   All functions returns nil if successful and an error message if not"
  [req]
  {:status 200
   :headers {"Content-Type" "text/json"}
   :body (-> (let [p (partial get-parameter req)
                   data (:path-params req)]
               (json/write-str
                 (case (p :type)
                   "deposit" (let [result (deposit (p :amount) (p :currency-code) (p :user-email))]
                               (response-body result data))
                   "withdrawal" (let [result (withdraw (p :amount) (p :currency-code) (p :user-email))]
                                  (response-body result data))
                   "transfer" (let [result (transfer (p :amount) (p :currency-code) (p :user-email) (p :receiver-email))]
                                (response-body result data))))))})

; Balances
;====================================================================================================

(defn balances-handler
  "Returns all balances"
  [_]
  {:status 200
   :headers {"Content-Type" "text/json"}
   :body  (json/write-str (get-all-balances))})

(defn balance-handler
  "Returns the balance for a user and currency"
  [req]
  {:status 200
   :headers {"Content-Type" "text/json"}
   :body (-> (let [p (partial get-parameter req)]
               (json/write-str (get-balance (p :user-email) (p :currency-code)))))})
