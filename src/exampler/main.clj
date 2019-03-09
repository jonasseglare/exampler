(ns exampler.main
  (:require [clojure.spec.alpha :as spec]
            [exampler.tutorialize :refer [tutorialize]])
  (:gen-class))



(spec/def ::tutorialize (spec/cat :prefix #{"tutorialize"}
                                  :filename string?))

(spec/def ::cmd-arg (spec/alt :tutorialize ::tutorialize))

(spec/def ::cmd-args (spec/* ::cmd-arg))

(defn -main [& args]
  (let [parsed-args (spec/conform ::cmd-args args)]
    (when (= ::spec/invalid parsed-args)
      (throw (ex-info (str "Bad input args: " (spec/explain-str ::cmd-args args))
                      {:args args})))

    (doseq [[arg-type arg-data] parsed-args]
      (case arg-type
        :tutorialize (tutorialize (:filename arg-data))))))
