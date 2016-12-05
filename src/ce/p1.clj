(ns ce.p1
  (:require [ce.utils :refer :all]))

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
(def stochastic-prob (atom 0.8))
;; Número de individuos seleccionados por ronda en selección por torneo
(def tournament-size (atom 5))
;; Torneo con o sin reemplazamiento
(def replacement (atom true))
;; Pobabilidad de mezcla en crossover
(def crossover-prob (atom 0.6))
;; Número de generaciones tope para el experimento
(def max-generations (atom 100))
;; Número de generaciones sin mejora del fitness que se permiten antes de dar por acabado el exp.
(def idle-generations (atom 10))
;; Cada cuantas generaciones se reporta el estado al web-service en la nube para su visualización.
(def report-delta (atom 5))

;; Inicialmente, cada objeto lo represento por un mapa con claves
;; nam (nombre), val (valor) y vol (volumen).
;;  ej. {:nam "nombre" :val 1 :vol 3}
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
;; Por ejemplo, el individuo [true false true] representa meter en la mochila
;; el primer y tercer objetos (una vez reordenados con la función
;; arrange-objects)

;; Función que devuelve el valor de aptitud o conveniencia de un individuo.
;; Procedimiento:
;;  - se queda con los objetos que tiene en la mochila (gen true)
;;    ordenados por ratio descendente
;;  - acumula los valores de los objetos, mientras quepan en la mochila
;;  - devuelve el valor acumulado
;; Parámetros:
;;  - individual: el individuo
(defn fitness [individual]
  (->> individual
       (map-indexed (fn [i v] (if v (get @objects i))))
       (remove nil?)
       (reductions (fn [[acum-val acum-vol] o]
                     [(+ acum-val (:val o)) (+ acum-vol (:vol o))])
                   [0 0])
       (filter #(<= (last %) @pack-size))
       last
       first))

;; Devuelve la representación de un individuo como el conjunto de objetos
;; que introduce en la mochila.
(defn decode [individual]
  (let [objects (remove nil? (map-indexed (fn [i v] (when v (get @objects i))) individual))
        reds (reductions + (map :vol objects))]
    (take (count (take-while (partial >= @pack-size) reds)) objects)))

;; Genera un objeto a partir de sus propiedades.
;; - nam: nombre del objeto
;; - val: valor del objeto
;; - vol: volumen del objeto
(defn new-object
  ([[nam val vol]] (new-object nam val vol))
  ([nam val vol] {:nam nam :val val :vol vol}))

;; Genera un objeto aleatoriamente con valor entre 0 y max-val y
;; volumen entre 1 y max-vol.
;; - i: número de objeto
;; - max-val: valor máximo
;; - max-vol: volument máximo
(defn rand-object [i max-val max-vol]
  (new-object (str "object " i) (rand-int max-val) (inc (rand-int (dec max-vol)))))

;; Genera aleatoriamente un individuo.
(defn rand-individual []
  (repeatedly (count @objects) (fn [] (<= (rand) @rand-gen-prob))))

;; Genera aleatoriamente una población.
(defn rand-population []
  (repeatedly @population-size (fn [] (rand-individual))))

;; Selecciona el primer elmento de una lista con probabilidad stochastic-prob.
;; De no ser seleccionado, selecciona el primero del resto con probabilidad
;;  stochastic-prob también. Y así hasta agotar la lista.
(defn first-stochastic [col]
  (loop [[elem & more] col]
    (if (or (empty? more) (<= (rand) @stochastic-prob)) elem
      (recur more))))

;; Selecciona por torneo estocástico.
;;  - population: población de la que se selecciona
;;  - size: número de individuos a seleccionar
(defn tournament-stochastic [population size]
  (loop [population population
         selected []]
    (if (or (empty? population) (<= size (count selected))) selected
      (let [individual (->> population shuffle (take @tournament-size)
                            (pmap (fn [ind] [(fitness ind) ind]))
                            (sort-by first) reverse (map second) first-stochastic)]
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

;; Determina si se ha alcanzado el máxido de generaciones del problema.
;;  - generation: la generación en curso
(defn enough-generations? [generation]
  (<= @max-generations generation))

;; Variable donde almacenamos los mejores individuos de la historia reciente.
(def best-history (atom []))

;; Determina si se llevan demasiadas generaciones sin que aparezcan individuos
;; con fitness mejorado.
;;  - generation: la generación en curso
;;  - best: el mejor individuo de la generación
(defn enough-blockage? [generation best]
  (swap! best-history (fn [h] (take @idle-generations (concat [best] h))))
  (and (< @idle-generations generation)
       (<= (apply max (map fitness (butlast @best-history)))
           (fitness (last @best-history)))))

;; Determina si la evolución ha llegado a su fin, bien por haberse alcanzado
;; demasiadas generaciones, bien por llevar demasiadas generaciones sin que se
;; incremente el fitness.
;;  - generation: la generación en curso
;;  - best: el mejor individuo de la generación
(defn done? [generation best]
  (or (enough-generations? generation)
      (enough-blockage? generation best)))

;; Devuelve la razón por la que el algoritmo acaba, para poderla reportar.
(defn done-reason [generation best]
  (cond
    (enough-generations? generation) "Done! Enough generations lived"
    (enough-blockage? generation best) "Done! Too many generations without improving fitness"
    :else "Should never happen"))

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

;; Inicializa las variables de este namespace con los valores propios de
;; cada experimento que viene en config.
;; - config: un mapa con la configuración del experimento.
(defn set-config [config]
  (reset! pack-size (:pack-size config))
  (reset! objects (arrange-objects (:objects config)))
  (reset! rand-gen-prob (:rand-gen-prob config))
  (reset! population-size (:population-size config))
  (reset! stochastic-prob (:stochastic-prob config))
  (reset! tournament-size (:tournament-size config))
  (reset! replacement (:replacement config))
  (reset! crossover-prob (:crossover-prob config))
  (reset! max-generations (:max-generations config))
  (reset! idle-generations (:idle-generations config))
  (reset! report-delta (:report-delta config)))

;; Inicializa y lleva a cabo la evolución. Reporta el resultado.
;; Devuelve el mejor individuo encontrado.
;; - config: mapa con la configuración del experimento. Adopta esta forma:
;; {:pack-size 755
;;  :objects objects
;;  :population-size 50
;;  :tournament-size 5
;;  :replacement true
;;  :rand-gen-prob 5/10
;;  :stochastic-prob 8/10
;;  :crossover-prob 5/10
;;  :max-generations 200
;;  :idle-generations 20
;;  :report-delta 1
;;  :name "Nombre del experimento"}
(defn go-live [config]
  (set-config config)
  (loop [generation 0
         [best-parent & more :as parents] (->> (rand-population) (sort-by fitness) reverse)]
    (report-status config generation best-parent (fitness best-parent) "running" (decode best-parent))
    (if (done? generation best-parent)
      (do (report-status config generation best-parent (fitness best-parent)
                         (done-reason generation best-parent) (decode best-parent))
        best-parent)
      (let [[best-child & more :as offspring] (->> parents build-offspring (sort-by fitness) reverse)
            elitism? (< (fitness best-child) (fitness best-parent))
            elitism-offspring (if elitism?
                                (butlast (concat [best-parent] offspring))
                                offspring)]
        (recur (inc generation) elitism-offspring)))))


;; Inicializa y lleva a cabo la evolución. Reporta el resultado.
;; Devuelve el mejor individuo encontrado.
;; - path: ruta al fichero con objetos y configuración a utilizar
;; El ficero tiene que tener un formato como el siguiente:
;; {:pack-size 1234
;;  :population-size 2
;;  :tournament-size 2
;;  :replacement true
;;  :rand-gen-prob 1/10
;;  :stochastic-prob 1/10
;;  :crossover-prob 1/10
;;  :max-generations 200
;;  :idle-generations 10
;;  :report-delta 1
;;  :name "amanas: Todo desde fichero 1"
;;  :objects [["objeto 1" 150 9]
;;            ["objeto 2" 120 8]
;;            ...]}
(defn go-live-from-file [path]
  (let [data (read-string (slurp path))
        data (assoc data :objects (map new-object (:objects data)))]
    (decode (go-live data))))
