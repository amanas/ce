(ns ce.p1-test
  (:require [clojure.test :refer :all]
            [ce.p1 :refer :all]))

;; Genera un objeto aleatoriamente con valor entre 0 y max-val y
;; volumen entre 1 y max-vol.
;; - i: número de objeto
;; - max-val: valor máximo
;; - max-vol: volument máximo
(defn rand-object [i max-val max-vol]
  (new-object (str "object " i)
              (rand-int max-val)
              (inc (rand-int (dec max-vol)))))

;; Plantilla para experimentos con objetos autogenerados
;; y configuración ad-hoc
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
        :name "amanas: Autogenerado 1"}
       go-live
       decode))

(comment (go-live-from-file "resources/data/rossetacode.scm"))


