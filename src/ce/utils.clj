(ns ce.utils
  (:require [clj-http.client :as http]
            [cheshire.core :as json])
  (:import [java.util Date]))

;; Variable dónde almacenamos el histórico de la evolución de un experimento
(def status (atom nil))

;; Devuelve el momento actual desde la época
(defn now [] (.getTime (java.util.Date.)))

;; Envía el estado a un web service en la nube para permitir la visualización
;; del estado del experimento en tiempo real.
;; Ábrase http://amanas.ml/ce/resources/dashboard.html en un navegador web.
(defn- post-status []
  (http/post "http://amanas.ml/ce/resources/backend.php"
             {:body (json/encode @status)
              :content-type :json
              :accept :json}))

;; Toma una instantánea del estado en el que se encuentra un experimento
;; - best: mejor individuo de la generación actual
;; - generation: generación por la que va la evolución del experimento
;; - fitness: la fitness alcanzada por el mejor individuo de la generación en curso
(defn report-status
  ([config generation best rel-fitness]
   (report-status config generation best rel-fitness nil))
  ([config generation best rel-fitness result]
   (reset! status (merge {:config (dissoc config :objects)
                          :best best
                          :result result
                          :generation generation
                          :current-time (now)
                          :best-rel-fitness rel-fitness}
                         (if (zero? generation)
                           {:status [["Generation" "Relative fitness"]]
                            :start-time (now)}
                           {:status (conj (:status @status) [generation rel-fitness])
                            :start-time (:start-time @status)})))
   (when (zero? (mod generation (:report-delta config)))
     (post-status))))



