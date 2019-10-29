(ns poznan-data.handler
  (:require
   [compojure.core :refer [GET defroutes]]
   [compojure.route :refer [resources]]
   [ring.util.response :as response :refer [resource-response]]
   [ring.middleware.reload :refer [wrap-reload]]
   [cheshire.core :as json]
   [clj-http.client :as http]
   [ring.middleware.json  :as json-middleware]
   [clojure.string :as string]
   [clojure.java.io :as io]
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as cske])
  (:import [com.google.transit.realtime GtfsRealtime$FeedMessage]))

(defn fetch-json [url]
  (let [resp (http/get url {:accept? :json})]
    (-> resp :body (json/parse-string true))))

(defn fetch-types []
  (map :slug (fetch-json "http://api.syngeos.pl/api/public/sensors")))

(defn fetch-sensors-by-type [type]
  (fetch-json (str "http://api.syngeos.pl/api/public/data/" type)))

(defn fetch-sensor [id]
  (fetch-json (str "https://api.syngeos.pl/api/public/data/device/" id)))

(defn ->sensor-entry [entry]
  (-> entry
      (dissoc :source :send-reports)
      (update :coordinates (fn [[lat lng]] {:lat lat :lng lng}))))

(def poznan-sensors [33 418 798 1017])
(def sensors-types ["pm10" "pm2_5" "caqi" "no2" "so2" "o3" "c6h6" "noise"])
(defn list-poznan-sensors []
  (map #(->> %
             fetch-sensor
             (cske/transform-keys csk/->kebab-case-keyword)
             ->sensor-entry)
       poznan-sensors))

(defn fetch-parkings [url conversion-fn]
  (let [csv (-> (http/get url) :body)
        entries (map conversion-fn (rest (string/split csv #"\n")))
        grouped (group-by :name entries)]
    (->> grouped (map (fn [[k v]] (->> v (sort-by :date) reverse first))))))

(def buffer-parking->coords
  {"Reymonta" {:lat 52.395126 :lng 16.887843}
   "Za Bramką" {:lat 52.405711 :lng 16.936920}
   "Rondo Kaponiera" {:lat 52.408595 :lng 16.913372}
   "Maratońska" {:lat 52.399400 :lng 16.930735}
   "Głogowska - przy dw. Zachodnim" {:lat 52.401506 :lng 16.909627}})
(defn ->buffer-parking-entry [csv-entry]
  (let [format (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss")
        [date free incoming outcoming name] (string/split csv-entry #";")]
    (.setTimeZone format (java.util.TimeZone/getTimeZone "UTC"))
    {:date (.parse format date)
     :free free
     :incoming incoming
     :outcoming outcoming
     :name name
     :position (get buffer-parking->coords name)}))

(def route-id->name (let [routes (-> "gtfs/routes.txt"
                                     io/resource
                                     slurp
                                     (string/split #"\n")
                                     rest)]
                      (into {} (for [route routes]
                                 (let [[route-id agency-id route-short-name route-long-name route-desc route-type route-color route-text-color] (.split route ",")]
                                   [route-id (-> route-long-name string/trim (string/replace "\"" ""))])))))

(def park-ride-parking->coords {"Szymanowskiego" {:lat 52.460880 :lng 16.916025}})
(defn ->park-ride-entry [csv-entry]
  (let [format (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss")
        [date free outcoming incoming name] (string/split csv-entry #";")]
    (.setTimeZone format (java.util.TimeZone/getTimeZone "UTC"))
    {:date (.parse format date)
     :free free
     :incoming incoming
     :outcoming outcoming
     :name name
     :position (get park-ride-parking->coords name)}))

(defn parse-gtfs-url [url]
  (GtfsRealtime$FeedMessage/parseFrom (-> url
                                          java.net.URL.
                                          .openStream)))

(defn gtfs-entity->position [entity]
  (let [trip (-> entity .getVehicle .getTrip)
        route-id (.getRouteId trip)
        trip-id (.getTripId trip)
        position (-> entity .getVehicle .getPosition)]
    {:route-id route-id
     :trip-id trip-id
     :name (route-id->name route-id)
     :position {:latitude (.getLatitude position)
                :longitude (.getLongitude position)
                :speed (.getSpeed position)}}))

(defn fetch-transit-data []
  (let [vehicle-positions-url "https://www.ztm.poznan.pl/pl/dla-deweloperow/getGtfsRtFile?file=vehicle_positions.pb"
        feed (parse-gtfs-url vehicle-positions-url)]
    (map gtfs-entity->position (.getEntityList feed))))

(defroutes routes
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (GET "/sensors" [] (response/response (list-poznan-sensors)))
  (GET "/parkings/buffer" [] (response/response (fetch-parkings "https://www.ztm.poznan.pl/pl/dla-deweloperow/getBuforParkingFile" ->buffer-parking-entry)))
  (GET "/parkings/park-ride" [] (response/response (fetch-parkings "https://www.ztm.poznan.pl/pl/dla-deweloperow/getParkingFile" ->park-ride-entry)))
  (GET "/transit/positions" [] (response/response (fetch-transit-data)))
  (resources "/"))

(def dev-handler (-> #'routes wrap-reload))

(def handler (json-middleware/wrap-json-response routes))
