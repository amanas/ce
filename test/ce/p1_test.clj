(ns ce.p1-test
  (:require [clojure.test :refer :all]
            [ce.p1 :refer :all]))


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
          :objects (map (fn [i] {:val (rand-int 10) :vol (inc (rand-int 5))}) (range 200))})

