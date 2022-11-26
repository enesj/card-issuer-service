(ns app.domain.file-notifications
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]))

(def notifications-path "resources/notifications/")

(defn- make-notification-file-id
  "Returns a timestamp and adds random numbers to it to make it unique also in test environment"
  []
  (let [now (java.util.Date.)]
    (-> now
      (str "-" (rand-int 10000))
      (str/replace  " " "_"))))

(defn send-notification [data]
  (let [file (io/file (str notifications-path (make-notification-file-id) "-notification.txt"))]
    (spit file data)))

(defn count-notifications
  "Returns the number of notifications in the notifications folder"
  []
  (dec (count (file-seq (io/file notifications-path)))))

(defn delete-notifications-files
  "Deletes all files in the notifications folder"
  []
  (doseq [file (.listFiles (io/file notifications-path))]
    (when-not (.isDirectory file)
      (.delete file))))

(comment
  (delete-notifications-files)
  (count-notifications)
  (make-notification-file-id)
  (send-notification 1 "Hello")

  ,)

