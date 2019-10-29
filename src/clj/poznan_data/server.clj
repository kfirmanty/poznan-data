(ns poznan-data.server
  (:require [poznan-data.system :as system])
  (:gen-class))

(defonce s (atom nil))

(defn start [] (reset! s (system/start)))

(defn stop [] (swap! s system/stop))

(defn restart []
  (stop)
  (start))

(defn -main [& args]
  (start))
