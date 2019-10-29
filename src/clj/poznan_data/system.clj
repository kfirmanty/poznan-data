(ns poznan-data.system
  (:require [com.stuartsierra.component :as component]
            [system.components.jetty :as jetty]
            [poznan-data.handler :as handler]))

(defn- system []
  (component/system-map
   :web (jetty/new-web-server 3000 handler/handler)))

(defn start []
  (component/start (system)))

(defn stop [system]
  (component/stop system))
