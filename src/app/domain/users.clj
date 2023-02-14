(ns app.domain.users
  (:require
   [app.db :as db]
   [clojure.string :as str]
   [next.jdbc :as nj]))

(defn add-user [firstname surname email]
  (tap> {"Adding user: " firstname surname email})
  (nj/execute! db/datasource
               ["INSERT INTO users (firstname, surname, email) VALUES (?, ?, ?) returning firstname, surname, email"
                (str/capitalize firstname) (str/capitalize surname) (str/lower-case email)]))

(defn get-all-users []
  (nj/execute! db/datasource ["SELECT firstname, surname, email FROM users"]))

(defn get-user-id-by-email [email]
  (-> (nj/execute-one! db/datasource ["SELECT id FROM users WHERE email = ?" email])
      :users/id))

(comment
  (get-user-id-by-email "john.smit@gmail.com")
  (get-all-users)
  (add-user "irma" "jakic" "irma.jakic@gmail.com"))

