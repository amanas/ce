(ns ce.p1
   (:require [clj-http.client :as http]
             [cheshire.core :as json]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Actividad 1: Problema de la mochila binario ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; Defino en primer lugar los meta-parámetros del algoritmo.
;; Y los inicializo con valores por defecto.
;; Para cada experimentación del algortimo actualizaremos sus
;; valores convenientemente

;; El tamaño de la mochila
(def pack-size (atom 10))
;; Los objetos - los inicializaré después de definir la función arrange-objects
(def objects (atom []))
;; Probabilidad de activación de los genes cuando se genera un individuo
(def rand-gen-prob (atom 0.6))
;; Tamaño inicial de la población
(def population-size (atom 10))
;; Probabilidad utilizada para selección estocástica
(def first-stochastic-prob (atom 0.8))
;; Número de individuos seleccionados por ronda en selección por torneo
(def tournament-round-size (atom 5))
;; Torneo con o sin reemplazamiento
(def replacement (atom true))
;; Pobabilidad de mezcla en crossover
(def crossover-prob (atom 0.6))
;; Número de generaciones tope para el experimento
(def generations-threshold (atom 100))
;; Umbral de calidad que, una vez alcanzado, da por finalizado el experimento
(def fitness-threshold (atom 95/100))


;; Inicialmente, cada objeto lo represento por un mapa con claves
;; nam (nombre), val (valor) y vol (volumen).
;;  ej. {:nam "objeto 1" :val 1 :vol 3}
;; Necesitamos una función que genere una estructura con los objetos iniciales
;; del problema optimizada para accesos y para economía de computación.
;; Para ello:
;;  1. enriquezco los objetos iniciales con el valor del ratio
;;     (val/vol) para no tener que recalcularlo en cada acceso.
;;  2. ordeno en un array el conjunto resultante por ratio de mayor a menor
;;  3. genero un mapa con los índices de los objetos en el array (como claves)
;;     que  apuntan a los propios objetos (como valores)
;; Parámetros:
;;  - objects: los objetos iniciales definidos en el problema
(defn arrange-objects [objects]
  (->> objects
       (map #(assoc % :ratio (/ (:val %) (:vol %))))
       (sort-by :ratio)
       reverse
       (map-indexed (fn [i v] {i v}))
       (apply merge)))

;; Decido además representar cada individuo como un vector de genes binarios.
;; Por ejemplo, este individuo representa meter en la mochila
;; el primer y tercer objetos (una vez reordenados con la función
;; arrange-objects): [true false true]

;; Función que devuelve el valor de aptitud o conveniencia de un individuo.
;; Procedimiento:
;;  - se queda con los objetos que tiene en la mochila (gen true)
;;    ordenados por ratio descendente
;;  - acumula los valores de los objetos, mientras quepan en la mochila
;;  - devuelve el acumulado
;; Parámetros:
;;  - individual: el individuo
(defn fitness [individual]
  (->> individual
       (map-indexed (fn [i v] (if v (:val (get @objects i)) 0)))
       (reductions +)
       (filter #(<= % @pack-size))
       last))

;; Genera aleatoriamente un individuo.
(defn rand-individual []
  (repeatedly (count @objects) (fn [] (<= (rand) @rand-gen-prob))))

;; Genera aleatoriamente una población.
(defn rand-population []
  (repeatedly @population-size (fn [] (rand-individual))))


;; Selecciona el primer elmento de una lista con probabilidad first-stochastic-prob.
;; De no ser seleccionado, selecciona el primero del resto con probabilidad
;;  first-stochastic-prob también. Y así hasta agotar la lista.
(defn first-stochastic [col]
  (loop [[elem & more] col]
    (if (or (empty? more) (<= (rand) @first-stochastic-prob)) elem
      (recur more))))

;; Selecciona por torneo estocástico.
;;  - population: población de la que se selecciona
;;  - size: número de individuos a seleccionar
(defn tournament-stochastic [population size]
  (loop [population population
         selected []]
    (if (or (empty? population) (<= size (count selected))) selected
        (let [individual (->> population shuffle (take @tournament-round-size)
                              (sort-by fitness) reverse first-stochastic)]
          (recur (if @replacement (remove (partial = individual) population) population)
                 (conj selected individual))))))

;; Cruza dos padres por un punto o devuelve los padres tal cual, dependiendo
;; de la probabilidad de cruze.
;;  - parent1: primer padre
;;  - parent2: segundo padre
(defn crossover-one-point [parent1 parent2]
  (if (<= (rand) @crossover-prob)
      (let [point (inc (rand-int (dec (count parent1))))]
        [(concat (take point parent1) (drop point parent2))
         (concat (take point parent2) (drop point parent1))])
      [parent1 parent2]))

;; Muta los genes de un individuo atendiendo a una probabilidad de mutación
;; data por 1/número de objetos del individuo.
;;  - individual: el individuo a mutar
(defn mutate [individual]
  (let [p (/ 1 (count individual))]
    (map (fn [o] (if (<= (rand) p) (not o) o)) individual)))

;; Determina si la evolución de un individuo es superior al umbral del problema.
;;  - individual: el individuo
(defn enough-fitness? [individual]
  (<= @fitness-threshold (/ (fitness individual) @pack-size)))

;; Determina si se ha alcanzado el máxido de generaciones del problema.
;;  - generation: la generación en curso
(defn enough-generations? [generation]
  (<= @generations-threshold generation))

;; Determina si la evolución ha llegado a su fin, bien por haberse alcanzado
;; demasiadas generaciones, bien por haberse alcanzado un individuo con
;; fitness suficiente
;;  - generation: la generación en curso
;;  - best: el mejor individuo de la generación
(defn done? [generation best]
  (or (enough-generations? generation)
      (enough-fitness? best)))

;; Construye la nueva generación a partir de la generación actual
;; aplicando el modelo generacional.
;;  - population: la población actual
(defn build-offspring [population]
  (loop [offspring []]
    (if (<= (count population) (count offspring)) offspring
      (recur (->> (tournament-stochastic population 2)
                  (apply crossover-one-point)
                  (map mutate)
                  (concat offspring))))))



(def status (atom nil))

(defn post-status []
  (http/post "http://amanas.ml/ce/p1/backend.php"
             {:body (json/encode @status) ;;"{\"json\": \"input\"}"
              :content-type :json
              :accept :json}))

(defn reset-status [config]
  (reset! status {:config config
                  :status [["Generation" "Fitness"]]}))

(defn report-status [best generation fitness]
  (swap! status update-in [:status] conj [generation fitness])
  (swap! status assoc-in [:best] best)
  (when (= 0 (mod generation 5)) (post-status)))




;; Inicializa y lleva a cabo la evolución.
(defn go-live []
  (reset-status :config)
  (loop [generation 0
         [best-parent & more :as sorted-population] (->> (rand-population)
                                                         (sort-by fitness)
                                                         reverse)]
    (report-status best-parent generation  (/ (fitness best-parent) @pack-size))
    (if (done? generation best-parent)
      (do (prn (str "Evolution done in " generation " generations"))
          (prn (str "Best individual found: " (vec best-parent)))
          (prn (str "Fitness of best individual: " (fitness best-parent)))
          (prn (str "Relative fitness reached: " (/ (fitness best-parent) @pack-size)))
          best-parent)
      (let [[best-child & more :as sorted-offspring] (->> (build-offspring sorted-population)
                                                          (sort-by fitness)
                                                          reverse)
            elitism? (< (fitness best-child) (fitness best-parent))
            sorted-elitism-offspring (if elitism?
                                       (butlast (concat [best-parent] sorted-offspring))
                                       sorted-offspring)]
        (recur (inc generation) sorted-elitism-offspring)))))





(defn rand-object [i]
  {:nam (str "objeto " i) :val (rand-int 300) :vol (inc (rand-int 20))})



(reset! pack-size 55555)
(reset! rand-gen-prob 0.6)
(reset! population-size 100)
(reset! first-stochastic-prob 0.8)
(reset! tournament-round-size 10)
(reset! replacement true)
(reset! crossover-prob 0.8)
(reset! generations-threshold 50)
(reset! fitness-threshold 100/100)
(reset! objects (arrange-objects (map rand-object (range 200))))




;; (go-live)
















