(ns app.domain.transactions
  (:require
   [app.db :as db]
   [app.config :refer  [config]]
   [app.domain.users :refer [get-user-id-by-email]]
   [next.jdbc :as nj]))


(def db-config
  (:db-config config))

(defmulti add-transaction
  "add transaction to database and return response. If transaction is a transfer requires additional parameter 'receiver-email'"
  (fn [type & [_]]
    (if (= type :transfer)
      type
      :default)))

(defmethod  add-transaction :transfer
  ;Multi-arity method can be called with 4 or 5 arguments. Datasource argument is needed when called inside 'with-transaction'
  ([type amount currency-code user-id receiver-id]
   (add-transaction [type amount currency-code user-id receiver-id db/datasource]))
  ([type amount currency-code user-id receiver-id ds]
   (let [type (name type)
         ds (or ds ds db/datasource)]
     (nj/execute! ds
                  ["INSERT INTO transactions (amount, currency_code, user_id, type, receiver_id) VALUES (?::integer, ?, ?::uuid, ?::transaction_type, ?)
                 returning amount, currency_code, user_id, type, receiver_id"
                   amount currency-code user-id type receiver-id]))))

(defmethod add-transaction :default
  ;Multi-arity method can be called with 4 or 5 arguments. Datasource argument is needed when called inside 'with-transaction'
  ([type amount currency-code user-id]
   (add-transaction type amount currency-code user-id db/datasource))
  ([type amount currency-code user-id ds]
   (let [type (name type)
         ds (or ds ds db/datasource)]
     (nj/execute! ds
       ["INSERT INTO transactions (amount, currency_code, user_id, type) VALUES (?::integer, ?, ?::uuid, ?::transaction_type )
                 returning amount, currency_code, user_id, type, receiver_id"
        amount currency-code user-id type]))))

(defn get-all-transactions
  "get all transactions from database"
  []
  (nj/execute! db/datasource ["SELECT amount, currency_code, user_id, type, receiver_id, description FROM transactions"]))

(def balances-sql
  ;SQL query string to get all balances for all users
  "with balance as (select user_id,
                 currency_code,
                 sum(CASE
                         WHEN type = 'deposit' THEN amount
                         ELSE - amount
                     END
                     )
                     as balance
          from transactions
          group by user_id, currency_code
          union all
          select receiver_id,
                 currency_code,
                 sum(CASE
                         WHEN type = 'transfer' THEN amount
                         ELSE 0
                     END
                     )
                     as balance
          from transactions
          group by receiver_id, currency_code
          having receiver_id is not null
          )
          select user_id,
                 currency_code,
                 sum(balance) as balance
          from balance
          group by user_id, currency_code")

(defn get-all-balances
  ;get all balances for all users, multi-arity function
  ([] (get-all-balances db/datasource))
  ([ds]
   (nj/execute! ds
                [(str balances-sql " order by user_id, currency_code")])))

(defn get-balance
  ; get balance for user with given email and currency code, , multi-arity function
  ([user-email currency-code]
   (get-balance db/datasource user-email currency-code))
  ([ds email currency-code]
   (let [user-id (get-user-id-by-email email)]
     (nj/execute-one! ds
                      [(str balances-sql " HAVING user_id = ?::uuid AND currency_code = ?")
                       user-id currency-code]))))

(defn broke-connection-in-transaction
  "This function is used to test transaction rollback.
   Connection to database is closed in the between of insert and select commands and insert is rolled back"
  [amount currency-code email]
  (let [user-id (get-user-id-by-email email)
        conn (nj/get-connection (-> db-config (dissoc :db-name) db/make-jdbc-url))]
    (try
      (nj/with-transaction [ds db/datasource]
        (add-transaction :deposit amount currency-code user-id)
        (nj/execute! conn
          [(str "ALTER DATABASE " (:db-name db-config) " ALLOW_CONNECTIONS = false")])
        (get-balance email currency-code))
      (catch Exception e (.getMessage e))
      (finally
        (nj/execute! conn
          [(str "ALTER DATABASE " (:db-name db-config) " ALLOW_CONNECTIONS = true")])))))

(defn deposit
  "add deposit transaction to database"
  [amount currency-code email]
  (let [user-id (get-user-id-by-email email)]
    (add-transaction :deposit amount currency-code user-id))
  nil)


(defn withdraw
  "add withdraw transaction to database"
  [amount currency-code email]
  (let [user-id (get-user-id-by-email email)]
    (try
      (nj/with-transaction [ds db/datasource]
        (add-transaction :withdrawal amount currency-code user-id ds)
        (when (< (:balance (get-balance ds email currency-code)) 0)
          (throw (Exception. "Insufficient funds"))))
      (catch Exception e (.getMessage e)))))

(defn transfer
  "add transfer transaction to database"
  [amount currency-code user-email receiver-email]
  (let [user-id (get-user-id-by-email user-email)
        receiver-id (get-user-id-by-email receiver-email)]
    (try
      (nj/with-transaction [ds db/datasource]
        (add-transaction :transfer amount currency-code user-id receiver-id ds)
        (when (< (:balance (get-balance ds user-email currency-code)) 0)
          (throw (Exception. "Insufficient funds"))))
      (catch Exception e (.getMessage e)))))

(comment

  (try
    (throw (Exception. "Insufficient funds"))
    (catch Exception e (.getMessage e)))

  (deposit 10 "EUR" "john.smit@gmail.com")
  (transfer 200 "EUR" "john.smit@gmail.com" "jane.smith@gmail.com")
  (tap> (get-all-balances db/datasource))
  (tap> (get-all-transactions))
  (get-user-id-by-email "john.smit@gmail.com")
  (get-balance db/datasource "jane.smith@gmail.com" "eur")
  (get-all-balances)
  (deposit 200 "USD" "jane.smith@gmail.com")
  (withdraw 210 "USD" "jane.smith@gmail.com")
  (transfer db/datasource 100 "EUR" "jane.doe@gmail.com" "john.smit@gmail.com"))
