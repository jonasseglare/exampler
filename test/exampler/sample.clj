(ns exampler.sample)

[:begin-example "Square"]
;; Implement $s(x) = x^2$
(println "Define square")
(defn square [x] (* x x)) ;; [4]
[:end-example]

[:begin-example "Square-eval"]
(def res (square 3))
(println "The result is" res)
res ;; [:result]
[:end-example]
