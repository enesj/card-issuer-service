(ns app.domain.transactions-test
  (:require
   [app.db :as db]
   [app.domain.transactions :as transactions]
   [app.domain.users :as users]
   [cheshire.core :as json]
   [clj-http.client :as client]
   [app.domain.file-notifications :refer [count-notifications]]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [test-fixtures :refer [start-app-clear-db]]))

(use-fixtures :each start-app-clear-db)

(defn get-parsed-body
  [response]
  (-> (:body response)
      (json/parse-string true)))

(defn add-transaction
  "send authorization request and return response, simulate card issuer"
  [amount currency-code user-email type & [receiver-email]]
  (let [result (-> (client/post (str "http://localhost:3000/authorizations/" amount "/" currency-code "/" user-email "/" type (when receiver-email (str "/" receiver-email))))
                   get-parsed-body)]
    result))

(defn deposit [amount currency-code user-email]
  (add-transaction amount currency-code user-email "deposit"))

(defn withdraw [amount currency-code user-email]
  (add-transaction amount currency-code user-email "withdrawal"))

(defn transfer [amount currency-code user-email receiver-email]
  (add-transaction amount currency-code user-email "transfer" receiver-email))

(defn broken [amount currency-code user-email]
  (add-transaction amount currency-code user-email "broken"))

(defn user-emails []
  (->> (users/get-all-users)
       (mapv :users/email)))

(deftest deposit-withdrawal-test
  (let [user-email (first (user-emails))
        user-id (users/get-user-id-by-email user-email)]
    (testing "test deposit and withdrawal of different currencies and amounts"
      (is (= (deposit 100 "USD" user-email) {:is-authorised true}))
      (is (= (:balance (transactions/get-balance db/datasource user-email "USD"))
            200.00M))
      (is (= (deposit 100 "EUR" user-email) {:is-authorised true}))
      (is (= (:balance (transactions/get-balance db/datasource user-email "EUR"))
             200.00M))
      (is (= (deposit 100 "EUR" user-email) {:is-authorised true}))
      (is (= (:balance (transactions/get-balance db/datasource user-email "EUR"))
             300.00M))
      (is (= (withdraw 200 "EUR" user-email) {:is-authorised true}))
      (is (= (let [_ (withdraw 200 "EUR" user-email)]
               (:balance (transactions/get-balance db/datasource user-email "EUR")))
             100.00M))
      (is (= (withdraw 200 "EUR" user-email) {:is-authorised false
                                              :description "Insufficient funds"}))
      ;transaction is rolled back if insufficient funds, so balance should be the same as before transaction call.
      (is (= (:balance (transactions/get-balance db/datasource user-email "EUR"))
            100.00M))
      ;check the number of notifications sent in the test is equal to the number of all transactions: both successful and failed
      (is (= (count-notifications) 6)))))

(deftest broken-connection-in-transaction-test
  (is (= (:balance (transactions/get-balance db/datasource "john.smit@gmail.com" "USD")) 100.00M))
  (is (= (broken 10 "EUR" "john.smit@gmail.com")
        {:is-authorised false
         :description "FATAL: database \"db_test\" is not currently accepting connections"}))
  (is (= (:balance (transactions/get-balance db/datasource "john.smit@gmail.com" "USD")) 100.00M))
  (is (= (count-notifications) 1)))

(deftest transfer-test
  (let [sender-email (first (user-emails))
        receiver-email (second (user-emails))]
    (testing "test transfer of different currencies and amounts between users"
      (is (= (transfer 10 "USD" sender-email receiver-email) {:is-authorised true}))
      (is (= [(:balance (transactions/get-balance db/datasource sender-email "USD"))
              (:balance (transactions/get-balance db/datasource receiver-email "USD"))]
            [90.00M 110.00M]))
      (is (= (transfer 10 "EUR" sender-email receiver-email) {:is-authorised true}))
      (is (= (:balance (transactions/get-balance db/datasource sender-email "EUR"))
             90.00M))
      (is (= (transfer 10 "EUR" sender-email receiver-email) {:is-authorised true}))
      (is (= (:balance (transactions/get-balance db/datasource receiver-email "EUR"))
             120.00M))
      (is (= (transfer 10 "EUR" sender-email receiver-email) {:is-authorised true}))
      (is (= (:balance (transactions/get-balance db/datasource receiver-email "EUR"))
             130.00M))
      (is (= (transfer 20 "EUR" sender-email receiver-email) {:is-authorised true}))
      (is (= (:balance (transactions/get-balance db/datasource receiver-email "EUR"))
             150.00M))
      (is (=  (:balance (transactions/get-balance db/datasource sender-email "EUR")) 50.00M))
      (is (= (transfer 200 "EUR" sender-email receiver-email) {:is-authorised false
                                                               :description "Insufficient funds"}))
      ;transaction is rolled back if insufficient funds, so sender and receiver balances should be the same as before transaction call.
      (is (=  (:balance (transactions/get-balance db/datasource sender-email "EUR")) 50.00M))
      (is (=  (:balance (transactions/get-balance db/datasource receiver-email "EUR")) 150.00M))
      ;check the number of notifications sent in the test is equal to the number of all transactions: both successful and failed
      (is (= (count-notifications) 6)))))

(comment
  (= ({:amount 100.0, :currency_code "USD", :user_id "da59de61-8320-4473-aaf1-43cf62c889f1", :type "deposit", :receiver_id nil}
      {:amount 100.0, :currency_code "USD", :user_id "da59de61-8320-4473-aaf1-43cf62c889f1", :type "deposit", :receiver_id nil}))

  (let [user-email (first (user-emails))
        user-id (users/get-user-id-by-email user-email)]
    (withdraw 130 "EUR" user-email)))
