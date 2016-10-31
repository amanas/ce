(ns ce.p1-test
  (:require [clojure.test :refer :all]
            [ce.p1 :refer :all]))

;; Genera un objeto aleatoriamente con valor entre 0 y max-val y
;; volumen entre 1 y max-vol.
;; - max-val: valor máximo
;; - max-vol: volument máximo
(defn rand-object [max-val max-vol]
  {:val (rand-int max-val) :vol (inc (rand-int (dec max-vol)))})

(go-live {:pack-size 1000
          :population-size 10
          :rand-gen-prob 1/2
          :first-stochastic-prob 8/10
          :tournament-round-size 5
          :replacement true
          :crossover-prob 8/10
          :generations-threshold 200
          :fitness-threshold 98/100
          :blockage-delta 10
          :report-delta 1
          :objects (repeatedly 10 #(rand-object 20 20))})


