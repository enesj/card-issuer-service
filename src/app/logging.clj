(ns app.logging
  (:require
   [app.config :refer  [config]]
   [clojure.pprint]
   [io.aviso.ansi :as ansi]
   [io.aviso.exception]
   [io.aviso.logging :as logging]
   [taoensso.timbre :as log]
   taoensso.timbre.tools.logging))

(defn default-logging-config [data]
  (let [color (case (:level data)
                :info ansi/white
                :warn ansi/bold-white
                :error ansi/magenta
                ansi/blue)]
    (update data :vargs (partial mapv (fn [y] (color (if (string? y)
                                                       y
                                                       (with-out-str (do (println)
                                                                         (clojure.pprint/pprint y))))))))))

(defn init! []
  (log/merge-config! (:logging config))
  (taoensso.timbre.tools.logging/use-timbre)
  (logging/install-uncaught-exception-handler)
  (log/merge-config! {:middleware [default-logging-config]})
  (alter-var-root #'io.aviso.exception/*app-frame-names* (constantly [#"app.*"]))
  (alter-var-root #'io.aviso.exception/*default-frame-rules* (fn [val] (into val [[:package "java.lang" :hide]
                                                                                  [:package "org.eclipse.jetty.io" :hide]
                                                                                  [:package "org.eclipse.jetty.util.thread.strategy" :hide]
                                                                                  [:package "org.eclipse.jetty.server" :hide]
                                                                                  [:package "org.eclipse.jetty.server.handler" :hide]]))))
