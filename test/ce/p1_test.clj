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
(comment)
(->> {:name "amanas: Autogenerado 5.4"
      :pack-size 755
      :objects (map #(rand-object % 100 100) (range 20))
      :population-size 100
      :tournament-round-size 20
      :replacement true
      :rand-gen-prob 5/10
      :first-stochastic-prob 8/10
      :crossover-prob 5/10
      :generations-threshold 200
      :blockage-delta 5
      :report-delta 1}
     go-live
     decode)


(comment (go-live-from-file "resources/data/rossetacode.scm"))


