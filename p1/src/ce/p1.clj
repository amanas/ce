(ns ce.p1
  (:require [ce.utils :refer :all]
            [clojure.java.io :as io])
  (:gen-class))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Actividad 1: Problema de la mochila binario ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; Defino en primer lugar los meta-parámetros del algoritmo.
;; Y los inicializo con valores por defecto.
;; Para cada experimentación del algortimo actualizaremos sus
;; valores convenientemente
(def default-config
  {;; El tamaño de la mochila
    :pack-size 500 ;; [100,10.000] [10.000,1.000.000]
    ;; Probabilidad de activación de los genes cuando se genera un individuo
    :rand-gen-prob 1/2
    ;; Tamaño inicial de la población
    :population-size 10 ;; [10,100]
    ;; Probabilidad utilizada para selección estocástica en el torneo
    :stochastic-prob 9/10
    ;; Número de individuos seleccionados por ronda en selección por torneo
    :tournament-size 2 ;; Máxima explotación
    ;; Torneo con o sin reemplazamiento
    :replacement true
    ;; Pobabilidad de mezcla en crossover
    :crossover-prob 3/4 ;; [0.6,0.9]
    ;; Número de generaciones tope para el experimento
    :max-generations 100
    ;; Número de generaciones sin mejora del fitness que se permiten
    ;; antes de dar por acabado el exp.
    :idle-generations 5
    ;; Cada cuantas generaciones se reporta el estado al web-service
    ;;en la nube para su visualización.
    :report-delta 1})

;; En esta variable almacenaremos la configuración propia de cada ejecución
(def config (atom default-config))

;; Genera un objeto a partir de sus propiedades.
;; - nam: nombre del objeto
;; - val: valor del objeto
;; - vol: volumen del objeto
(defn new-object
  ([[nam val vol]] (new-object nam val vol))
  ([nam val vol] {:nam nam :val val :vol vol}))

;; Genera un objeto aleatoriamente con valor entre 1 y max-val y
;; volumen entre 1 y max-vol.
;; - i: número de objeto
;; - max-val: valor máximo
;; - max-vol: volument máximo
(defn rand-object [i max-val max-vol]
  (new-object (str "object " i)
              (inc (/ (rand-int (* 100 (dec max-val))) 100))
              (inc (/ (rand-int (* 100 (dec max-vol))) 100))))

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

;; En esta variable almacenaremos los objetos que se quieren introducir
;; en la mochila reordenados apropiadamente.
;; La inicializo con valores aleatorios pero en cada ejecución será
;; actualizada con los objetos propios de cada experimento.
(def objects (atom (arrange-objects (map #(rand-object % 100 100) (range 100)))))

;; Decido además representar cada individuo como un vector de genes binarios.
;; Por ejemplo, el individuo [true false true] representa meter en la mochila
;; el primer y tercer objetos (una vez reordenados con la función
;; arrange-objects)

;; Función que devuelve el valor de aptitud o conveniencia de un individuo y
;; el volumen de la mochila que rellena un individuo antes de desbordarla.
;; Procedimiento:
;;  - se queda con los objetos que tiene en la mochila (gen true)
;;    ordenados por ratio descendente
;;  - acumula los valores de los objetos, mientras quepan en la mochila
;;  - devuelve el valor acumulado y el volumen acumulado
;; Parámetros:
;;  - individual: el individuo
(defn fitness-and-volume [individual]
  (->> individual
       (map-indexed (fn [i v] (if v (get @objects i))))
       (remove nil?)
       (reductions (fn [[acum-val acum-vol] o]
                     [(+ acum-val (:val o)) (+ acum-vol (:vol o))])
                   [0 0])
       (filter #(<= (last %) (:pack-size @config) ))
       last))

;; Función que devuelve el valor de aptitud o conveniencia de un individuo
;; Parámetros:
;;  - individual: el individuo
(defn fitness [individual]
  (first (fitness-and-volume individual)))

;; Función que devuelve el volumen de mochila que rellena un individuo
;; antes de desbordarla.
;; Parámetros:
;;  - individual: el individuo
(defn volume [individual]
  (second (fitness-and-volume individual)))

;; Devuelve la representación de un individuo como el conjunto de objetos
;; que introduce en la mochila.
(defn decode [individual]
  (let [objects (remove nil? (map-indexed (fn [i v] (when v (get @objects i))) individual))
        reds (reductions + (map :vol objects))]
    (take (count (take-while (partial >= (:pack-size @config)) reds)) objects)))

;; Genera aleatoriamente un individuo.
(defn rand-individual []
  (repeatedly (count @objects) (fn [] (<= (rand) (:rand-gen-prob @config)))))

;; Genera aleatoriamente una población.
(defn rand-population []
  (repeatedly (:population-size @config) (fn [] (rand-individual))))

;; Selecciona el primer elmento de una lista con probabilidad stochastic-prob.
;; De no ser seleccionado, selecciona el primero del resto con probabilidad
;;  stochastic-prob también. Y así hasta agotar la lista.
(defn first-stochastic [col]
  (loop [[elem & more] col]
    (if (or (empty? more) (<= (rand) (:stochastic-prob @config))) elem
      (recur more))))

;; Selecciona por torneo estocástico.
;;  - population: población de la que se selecciona
;;  - size: número de individuos a seleccionar
(defn tournament-stochastic [population size]
  (loop [population population
         selected []]
    (if (or (empty? population) (<= size (count selected))) selected
      (let [individual (->> population shuffle (take (:tournament-size @config))
                            (pmap (fn [ind] [(fitness ind) ind]))
                            (sort-by first) reverse (map second) first-stochastic)]
        (recur (if (:replacement @config)
                 population
                 (remove (partial = individual) population))
               (conj selected individual))))))

;; Cruza dos padres por un punto o devuelve los padres tal cual, dependiendo
;; de la probabilidad de cruze.
;;  - parent1: primer padre
;;  - parent2: segundo padre
(defn crossover-one-point [parent1 parent2]
  (if (<= (rand) (:crossover-prob @config))
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
(defn too-much-generations? [generation]
  (<= (:max-generations @config) generation))

;; Variable donde almacenamos los mejores individuos de la historia reciente.
;; Se utiliza para determinar si han pasado demasiadas generaciones sin
;; que se incremente el fitness del mejor individuo.
(def best-history (atom []))

;; Determina si se llevan demasiadas generaciones sin que aparezcan individuos
;; con fitness mejorado.
;;  - generation: la generación en curso
;;  - best: el mejor individuo de la generación
(defn too-much-idle? [generation best]
  (swap! best-history (fn [h] (take (:idle-generations @config) (concat [best] h))))
  (and (< (:idle-generations @config) generation)
       (<= (apply max (map fitness (butlast @best-history)))
           (fitness (last @best-history)))))

;; Determina si la evolución ha llegado a su fin, bien por haberse alcanzado
;; demasiadas generaciones, bien por llevar demasiadas generaciones sin que se
;; incremente el fitness del mejor individuo.
;;  - generation: la generación en curso
;;  - best: el mejor individuo de la generación
(defn done? [generation best]
  (or (too-much-generations? generation)
      (too-much-idle? generation best)))

;; Devuelve la razón por la que el algoritmo acaba, para poderla reportar.
(defn done-reason [generation best]
  (cond
    (too-much-generations? generation) "Done! Enough generations lived"
    (too-much-idle? generation best)   "Done! Too many generations without improving fitness"
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

;; Inicializa y lleva a cabo la evolución. Reporta el resultado.
;; Devuelve el mejor individuo encontrado.
;;  - objects: los objetos a introducir en la mochila.
;;  - config: mapa con la configuración del experimento.
;; La configuración adopta esta forma:
;; {:pack-size 500
;;  :rand-gen-prob 1/2
;;  :population-size 10
;;  :stochastic-prob 9/10
;;  :tournament-size 2
;;  :replacement true
;;  :crossover-prob 3/4
;;  :max-generations 100
;;  :idle-generations 5
;;  :report-delta 1
;;  :name "Nombre del experimento"}
;; Los objetos son un array con esta forma:
;; [["nombre 1" valor-1 volumen-1]
;;  ["nombre 2" valor-2 volumen-2]
;;  ...
;; ]
(defn go-live [conf objs]
  (reset! config conf)
  (reset! objects (arrange-objects objs))
  (loop [generation 0
         [best-parent & more :as parents] (->> (rand-population) (sort-by fitness) reverse)]
    (report-status @config generation best-parent (fitness best-parent)
                   (volume best-parent) "running" (decode best-parent))
    (if (done? generation best-parent)
      (do (report-status @config generation best-parent
                         (fitness best-parent) (volume best-parent)
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
;;  - path: ruta al fichero con objetos y configuración a utilizar
;;  - config-override: parámetros de la configuración indicada en el fichero
;;                     que se desean sobreescribir en esta ejecución. Tiene el mismo
;;                     formato que config.
;; El ficero tiene que tener un formato como el siguiente:
;; {:config {:pack-size 500
;;           :rand-gen-prob 1/2
;;           :population-size 10
;;           :stochastic-prob 9/10
;;           :tournament-size 2
;;           :replacement true
;;           :crossover-prob 3/4
;;           :max-generations 100
;;           :idle-generations 5
;;           :report-delta 1
;;           :name "amanas: Todo desde fichero 1"}
;;  :objects [["objeto 1" 150 9]
;;            ["objeto 2" 120 8]
;;            ...]}
(defn go-live-from-file [path & [config-override]]
  (let [data (read-string (slurp path))
        config (merge (:config data) config-override)
        objects (map new-object (:objects data))]
    (decode (go-live config objects))))

;; Función que permite ejecutar el algoritmo invocando el jar ejecutable desde
;; una consola.
;; El comando para llamar al algoritmo es:
;; java -jar ejecutable.jar simple|complex tournament-size
;; Por ejemplo, se puede llamar con:
;; java -jar ejecutable.jar simple 5
;; Esta llamada ejecutará el experimento, cuya evolución puede verse en:
;; http://amanas.ml/ce/status.html
;; seleccionando en el combobox el experimento con nombre:
;; profe: simple - 5
(defn -main [& [type tour :as args]]
  (let [config {:name (format "profe: %s - %s" type tour)
                :tournament-size (read-string tour)}]
    (case type
      "simple"  (clojure.pprint/pprint
                  (decode (go-live-from-file (io/resource "data/simple.edn")  config)))
      "complex" (clojure.pprint/pprint
                  (decode (go-live-from-file (io/resource "data/complex.edn") config)))
      (prn "type must be 'simple' or 'complex'. '" type "' provided.")))
  (System/exit 0))
