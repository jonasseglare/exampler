(ns exampler.core
  (:import [java.io BufferedReader StringReader])
  (:require [clojure.spec.alpha :as spec]
            [clojure.string :as cljstr]))

;;;------- Spec for the lines -------
(defn begin-ex-str? [x]
  (.contains x "[:begin-example"))
(defn end-ex-str? [x]
  (.contains x "[:end-example"))
(defn non-begin-end-ex-str? [x]
  (and (not (begin-ex-str? x))
       (not (end-ex-str? x))))

(spec/def ::begin-ex-line begin-ex-str?)
(spec/def ::end-ex-line end-ex-str?)
(spec/def ::regular-line non-begin-end-ex-str?)

(spec/def ::regular-lines (spec/* ::regular-line))
(spec/def ::line-block (spec/cat :begin ::begin-ex-line
                                 :data ::regular-lines
                                 :end ::end-ex-line))
(spec/def ::file (spec/cat :init ::regular-lines
                           :blocks
                           (spec/*
                            (spec/cat :block ::line-block
                                      :space ::regular-lines))))

(defn parse-lines [lines]
  (let [result (spec/conform ::file lines)]
    (if (= result ::spec/invalid)
      (throw (ex-info (str "Failed to parse file:\n"
                           (spec/explain-str
                            ::file
                            lines))
                      {}))
      result)))

(defn parse-annot [annot-str]
  (read-string annot-str))

(defn parse-annotated-line [l]
  (let [[entire code annot]
        (re-matches #"^(.*);;\s*\[(\S+)\]\s*$" l)]
    (if entire
      {:code code
       :annot (parse-annot annot)}
      {:code l})))

(defn parse-annotated-lines [block]
  (update
   block
   :data
   (partial map parse-annotated-line)))

(defn parse-file-str [s]
  (->> s
       cljstr/split-lines
       parse-lines
       :blocks
       (map (comp parse-annotated-lines :block))))

(defn parse-file [filename]
  (-> filename
      slurp
      parse-file-str))

;;;------- Spec for the parsed forms -------
(spec/def ::settings map?)

(spec/def ::header (spec/spec
                    (spec/cat :prefix #{:begin-example}
                              :label string?
                              :settings (spec/? ::settings))))

(def end-block [:end-example])

(spec/def ::not-end (partial not= end-block))

(spec/def ::end #{end-block})

(spec/def ::block (spec/cat
                   :header ::header
                   :data (spec/* ::not-end)
                   :suffix ::end))

(spec/def ::form (spec/alt :block ::block
                           :form any?))

(spec/def ::forms (spec/* ::form))

(defn tag? [label x]
  (and (vector? x)
       (= label (first x))))

(def begin-example? (partial tag? :begin-example))
(def end-example? (partial tag? :end-example))



(defn try-read-form [forms]
  (let [
        [f & forms] forms]
    (if (vector? f)
      (let [[prefix label settings] f]
        (if (= :begin-example prefix)
          (let [data (take-while (complement end-example?) forms)]
            [{:header {:label label
                       :settings settings}
              :data data}
             (drop (inc (count data)) forms)]))))))

(defn parse-forms [forms]
  (loop [forms forms
         dst []]
    (if (empty? forms)
      dst
      (if-let [[result forms] (try-read-form forms)]
        (recur forms (conj dst [:block result]))
        (recur (rest forms) (conj dst [:form (first forms)]))))))


(def ^:dynamic state nil)

(def empty-state {:samples []})

(defn add-block [blk]
  (assert state)
  (swap! state (fn [x] (update x :samples conj blk))))

(defn apply-transform [settings k data]
  {:pre [(map? settings)
         (keyword? k)]}
  (if-let [f (:transform (get settings k))]
    (do
      (assert (fn? f))
      (f data))
    data))

(defmacro capture [settings forms]
  `(let [value# (promise)
         settings# ~settings
         output# (binding [*print-length* (::print-length settings#)]
                   (with-out-str
                     (deliver
                      value#
                      (do ~@forms))))]
     (add-block {:label (:label settings#)
                 :value (apply-transform settings#
                                         ::value (deref value#))
                 :output (apply-transform settings#
                                          ::output output#)
                 :settings settings#})
     value#))

(defn read-clj-data [data tmp-ns]
  (println "Read clj data")
  (binding [*ns* (create-ns tmp-ns)] 
    (let [result (read-string
                  (str "["
                       data
                       "]"))]
      (println "Got result")
      result)))

(defn forms-to-string [forms]
  (binding [*print-length* nil
            *print-level* nil]
    (cljstr/join "\n\n" (map pr-str forms))))

(defn load-forms [forms]
  (println "Load forms")
  (-> forms
      forms-to-string
      load-string))

(defn import-sub-settings [settings key-pairs src]
  (let [output 
        (reduce
         (fn [dst [src-key dst-key]]
           (update dst dst-key
                   (fn [x]
                     (merge x (get src src-key)))))
         settings
         key-pairs)]
    output))

(defn render-form [settings [form-type form-data]]
  (case form-type
    :block `(capture ~(let [header (:header form-data)]
                        (println "Annotating"
                                 (:label header))
                        (merge settings
                               (import-sub-settings
                                settings
                                [[:output ::output]
                                 [:code ::code]
                                 [:value ::value]]
                                (:settings header))
                               {:label (:label header)}))
                     ~(:data form-data))
    form-data))

(defn perform-annotations [forms settings]
  (println "Conforming it with a spec")
  (spit "forms.edn" forms)
  (let [parsed (parse-forms forms) ;;(spec/conform ::forms forms)
        ]
    (println "There are" (count parsed) "forms")
    (mapv
     (partial render-form settings)
     parsed)))

(defn extract-ns [filedata]
  (second (re-matches #"(?s)^\s*\(\s*ns\s+(\S+)\s+.*$" filedata)))

(defn load-annotated-file [filename settings]
  (let [file-data (slurp filename)
        ns-str (extract-ns file-data)
        ns-sym (symbol ns-str)]
    (println "The namespace is" ns-sym)
    (-> file-data
        (read-clj-data ns-sym)
        (perform-annotations settings)
        load-forms)))

(defn apply-code-transform [settings lines]
  (->> lines
       (map :code)
       (cljstr/join "\n")
       (apply-transform settings ::code)
       cljstr/split-lines))

(defn add-lines [state line-blocks]
  (update state
          :samples
          (fn [blocks]
            (assert (= (count blocks)
                       (count line-blocks)))
            (map (fn [block line-block]
                   (assoc block :code
                          (apply-code-transform
                           (:settings block)
                           (:data line-block))))
                 blocks line-blocks))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;;  Interface
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def default-settings {::print-length 8
                       ::output {:format :text}
                       ::code {}})

(defn extract
  ([filename]
   (extract filename default-settings))
  ([filename settings]
   (let [lines (parse-file filename)]
     (binding [state (atom empty-state)]
       (load-annotated-file filename settings)
       (add-lines (deref state) lines)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;;  Tests
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (def forms (-> "forms.edn" slurp read-string))
