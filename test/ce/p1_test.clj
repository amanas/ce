(ns ce.p1-test
  (:require [clojure.test :refer :all]
            [ce.p1 :refer :all]))

;; Plantilla para experimentos con objetos autogenerados
;; y configuración ad-hoc
(comment
  (->> {:name "amanas: experimento 1"
        :pack-size 755
        :objects (map #(rand-object % 10 10) (range 100))
        :population-size 100
        :tournament-size 5
        :replacement true
        :rand-gen-prob 5/10
        :stochastic-prob 8/10
        :crossover-prob 9/10
        :max-generations 200
        :idle-generations 10
        :report-delta 1}
       go-live
       decode))

;; Plantilla para experimentos con objetos
;; y configuración desde fichero
(comment
  (go-live-from-file "resources/data/rossetacode.scm"))


