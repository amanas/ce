(ns ce.p1-test
  (:require [clojure.test :refer :all]
            [ce.p1 :refer :all]))

;; Plantilla para experimentos con objetos autogenerados
;; y configuración ad-hoc
;; !!!Descomentar para su ejecución!!!
(comment
  (decode (go-live {:name "amanas: experimento 1"
                    :pack-size 500
                    :rand-gen-prob 1/2
                    :population-size 10
                    :stochastic-prob 9/10
                    :tournament-size 2
                    :replacement true
                    :crossover-prob 3/4
                    :max-generations 100
                    :idle-generations 5
                    :report-delta 1}
                   (map #(rand-object % 100 100) (range 100)))))

;; Experimento sencillo
;; 10 individuos
;; 100 objetos
;; con valor y volumen aleatoriamente entre [1, 100]
;; y con capacidad de la mochila un valor aleatorio en el intervalo real [100, 10.000]
(def simple-path "resources/data/simple.edn")
(comment
  (->> (range 100)
       (map #(rand-object % 100 100))
       (map vals)
       (map (partial into []))
       (into [])
       (assoc {:config {:name "amanas: simple"
                        :pack-size 517
                        :rand-gen-prob 1/2
                        :population-size 10
                        :stochastic-prob 9/10
                        :tournament-size 2
                        :replacement true
                        :crossover-prob 3/4
                        :max-generations 100
                        :idle-generations 5
                        :report-delta 1}} :objects)
       clojure.pprint/pprint
       with-out-str
       (spit simple-path)))

;; Ejecución de simple.edn con tamaño de torneo 2
;; Lo ejecuto 10 veces
(comment
  (doall
    (for [i (range 10)]
      (go-live-from-file simple-path
                         {:name (format "amanas: simple - tournament 2 - run %s" i)
                          :tournament-size 2}))))

;; Ejecución de simple.edn con tamaño de torneo 3
;; Lo ejecuto 10 veces
(comment
  (doall
    (for [i (range 10)]
      (go-live-from-file simple-path
                         {:name (format "amanas: simple - tournament 3 - run %s" i)
                          :tournament-size 3}))))

;; Ejecución de simple.edn con tamaño de torneo 5
;; Lo ejecuto 10 veces
(comment
  (doall
    (for [i (range 10)]
      (go-live-from-file simple-path
                         {:name (format "amanas: simple - tournament 5 - run %s" i)
                          :tournament-size 5}))))


;; Experimento complejo
;; 100 individuos
;; 10.000 objetos
;; con valor y volumen aleatoriamente entre [1, 100]
;; y con capacidad de la mochila un valor aleatorio en el intervalo real [10.000, 1.000.000]
(def complex-path "resources/data/complex.edn")
(comment)
(->> (range 1000)
     (map #(rand-object % 100 100))
     (map vals)
     (map (partial into []))
     (into [])
     (assoc {:config {:name "amanas: complex"
                      :pack-size 15237
                      :rand-gen-prob 1/2
                      :population-size 100
                      :stochastic-prob 9/10
                      :tournament-size 2
                      :replacement true
                      :crossover-prob 3/4
                      :max-generations 100
                      :idle-generations 5
                      :report-delta 1}} :objects)
     clojure.pprint/pprint
     with-out-str
     (spit complex-path))

;; Ejecución de complex.edn con tamaño de torneo 2
;; Lo ejecuto 10 veces
(comment)
(doall
  (for [i (range 1)]
    (go-live-from-file complex-path
                       {:name (format "amanas: complex - tournament 2 - run %s" i)
                        :tournament-size 2})))

