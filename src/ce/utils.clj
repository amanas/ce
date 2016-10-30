(ns ce.utils
   (:require [clj-http.client :as http]
             [cheshire.core :as json]))

;; Variable dónde almacenamos el histórico de la evolución de un experimento
(def status (atom nil))

;; Envía el estado a un web service en la nube para permitir la visualización
;; del estado del experimento en tiempo real
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
   (reset! status {:config config
                   :best best
                   :status (if (zero? generation)
                             [["Generation" "Relative fitness"]]
                             (conj (:status @status) [generation rel-fitness]))
                   :result result})
   (when (and generation
              (:post-delta config)
              (= 0 (mod generation (:post-delta config))))
     (post-status))))

