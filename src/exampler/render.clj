(ns exampler.render
  (:require [exampler.core :as core]
            [clygments.core :as clyg]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as cljstr]))

(def clygments-settings {:mathescape true})
(def clygments-keys (keys clygments-settings))

(defn prepare-file [label basic-name settings]
  (let [file (io/file (::output-path settings)
                      label
                      basic-name)]
    (io/make-parents file)
    file))

(defn join-lines [lines]
  (cljstr/join "\n" lines))

(defn get-code [sample]
  (join-lines (:code sample)))

(defn document-class? [x]
  (.contains x "\\documentclass"))

(defn begin-document? [x]
  (.contains x "\\begin{document}"))

(defn disp-seq [s]
  (binding [*print-length* 8]
    (println "SEQ:" s))
  s)

(defn get-preamble [code]
  (let [lines (cljstr/split-lines
               code)]
    (->> lines
         ;disp-seq
         (drop-while (complement document-class?))
         ;disp-seq
         rest
         (take-while (complement begin-document?))
         join-lines)))

(defn process-sample [sample settings]
  (let [label (:label sample)
        code (get-code sample)
        cl-settings (select-keys settings
                                 clygments-keys)]
    (println (format "Process example '%s'" label))
    (spit (prepare-file label "output.tex" settings)
          (clyg/highlight (:output sample)
                          (-> sample
                              :settings
                              ::core/output
                              :format)
                          :latex
                          cl-settings
                          ))
    (spit (prepare-file label "value.tex" settings)
          (clyg/highlight (with-out-str
                            (pp/pprint (:value sample)))
                          :clojure
                          :latex
                          cl-settings))
    (spit (prepare-file label "code.tex" settings)
          (clyg/highlight code
                          :clojure
                          :latex
                          cl-settings))
    (spit (prepare-file label "pre.tex" settings)
          (get-preamble
           (clyg/highlight code
                           :clojure
                           :latex
                           (merge
                            cl-settings
                            {:full true}))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;;  Interface
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def default-settings
  (merge core/default-settings
         clygments-settings
         {::output-path "./latex/exampler/"
          }))

(defn process-file
  ([filename]
   (process-file filename default-settings))
  ([filename settings]
   (let [data (core/extract filename settings)]
     (doseq [sample (:samples data)]
       (process-sample sample settings)))))

;;
(comment
  (do


    (def testfile "./test/exampler/sample.clj")

    (def data (process-file testfile))
    

    )


  )


