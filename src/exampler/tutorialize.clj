(ns exampler.tutorialize
  (:import [java.io File])
  (:require [clojure.string :as cljstr]
            [clojure.spec.alpha :as spec]))

(defn delimiting-line? [x]
  {:pre [(string? x)]}
  (and (= \" (first x))
       (cljstr/blank? (subs x 1))))

(defn swap-mode [mode]
  (case mode
    :text :code
    :code :text))

(defn acc-line [[result mode acc] line]
  (if (delimiting-line? line)
    (do
      [(conj result [mode acc])
       (swap-mode mode)
       []])
    [result mode (conj acc line)]))

(defn has-data? [[type data]]
  (not (empty? data)))

(defn parse [lines]
  (filter
   has-data?
   (first
    (reduce
     acc-line
     [[] :code []]
     lines))))

(defn replace-suffix [filename new-suffix]
  {:pre [(string? filename)]}
  (if-let [index (cljstr/last-index-of filename ".")]
    (str (subs filename 0 index) "." new-suffix)))

(defn surround-block [[type data]]
  (case type
    :text data
    :code (reduce into [] [["```clj"] data ["```"]])))

(defn render-markdown [parsed]
  (cljstr/join "\n" (reduce into [] (map surround-block parsed))))

(def default-settings {:generate-output-name #(replace-suffix % "md")})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;;  Interface
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn tutorialize
  ([source-file]
   (tutorialize source-file default-settings))
  ([source-file settings]
   (->> source-file
        slurp
        cljstr/split-lines
        parse
        render-markdown
        (spit ((:generate-output-name settings) source-file)))))


;; (tutorialize "/home/jonas/prog/clojure/geex/tutorial/core.clj")
