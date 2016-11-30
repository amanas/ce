(ns ce.p1-test
  (:require [clojure.test :refer :all]
            [ce.p1 :refer :all]))

;; Genera un objeto interpretable por mi algoritmo a partir de sus propiedades.
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

;; Plantilla para experimentos con datos autogenerados
(comment
  (->> {:pack-size 755
        :objects (map #(rand-object % 10 10) (range 200))
        :population-size 50
        :tournament-round-size 5
        :replacement true
        :rand-gen-prob 5/10
        :first-stochastic-prob 8/10
        :crossover-prob 5/10
        :generations-threshold 200
        :fitness-threshold 1
        :blockage-delta 20
        :report-delta 1
        :name "Experiment 1"}
       go-live
       decode))

;; Plantilla para experimentos con los datos obtenidos de
;; https://www.rosettacode.org/wiki/Knapsack_problem/0-1
(comment
  (let [data (read-string (slurp "resources/rossetacode.scm"))]
    (->> {:pack-size (:pack-size data)
          :objects (map new-object (:objects data))
          :population-size 2
          :tournament-round-size 2
          :replacement true
          :rand-gen-prob 1/10
          :first-stochastic-prob 1/10
          :crossover-prob 1/10
          :generations-threshold 200
          :fitness-threshold 1
          :blockage-delta 10
          :report-delta 1
          :name "Rosseta Code"}
         go-live
         decode)))
