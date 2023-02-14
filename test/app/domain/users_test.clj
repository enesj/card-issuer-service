(ns app.domain.users-test
  (:require
   [app.domain.users :as users]
   [cheshire.core :as json]
   [clj-http.client :as client]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [test-fixtures :refer [start-app-clear-db]]))

(use-fixtures :each start-app-clear-db)

(defn get-parsed-body
  [response]
  (-> (:body response)
      (json/parse-string true)))

(defn add-user [firstname surname email]
  (-> (client/post (str "http://localhost:3000/users/add/" firstname "/" surname "/" email))
      get-parsed-body))

(deftest add-user-test
  (testing "add-user"
    (is (= (users/add-user "john" "smith" "john.smith@gmail.com") [#:users{:firstname "John", :surname "Smith" :email "john.smith@gmail.com"}]))
    (is (= (add-user "irma" "jakic" "irma.jakic@gmail.com") {:firstname "Irma", :surname "Jakic" :email "irma.jakic@gmail.com"}))))

(deftest add-one-user-and-get-all-users-test
  (testing "adds one user and tests get-all-users returns this user plus the default user from the migrations"
    (users/add-user "irma" "jakic" "irma.jakic@gmail.com")
    (is (= (count (users/get-all-users)) 5))))

(deftest add-one-user-and-get-all-users-test-http
  (testing "adds one user and tests get-all-users returns this user plus the default user from the migrations"
    (add-user "irma" "jakic" "irma.jakic@gmail.com")
    (is (= (count (users/get-all-users)) 5))))

(deftest add-two-users-and-get-all-users-test
  (testing "adds two users and tests get-all-users returns this user plus the default user from the migrations"
    (users/add-user "John" "Smith" "john.smith@gmail.com")
    (users/add-user "irma" "jakic" "irma.jakic@gmail.com")
    (is (= (count (users/get-all-users)) 6))))

