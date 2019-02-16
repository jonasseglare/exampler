(ns exampler.utils
  (:require [clojure.string :as cljstr]
            [clojure.spec.alpha :as spec]))

(defn trim-line [s max-length]
  (if (< max-length (count s))
    (str (subs s 0 (max 0 (- max-length 4))) " ...")
    s))

(defn join-lines [lines]
  (cljstr/join "\n" lines))

(defn transform-lines [s f]
  (->> s
      cljstr/split-lines
      f
      join-lines))

(defn map-lines [s f]
  (transform-lines s (fn [lines] (map f lines))))

(defn filter-lines [s f]
  (transform-lines s (fn [lines] (filter f lines))))


(defn numbered-lines [lines]
  (map
   (fn [i line]
     {:index i
      :line line})
   (range)
   lines))

(defn nth-or-nil [src i]
  (if (< i (count src))
    (nth src i)))



(defn complete-input [input]
  (if (contains? input :pred)
    input
    (assoc input :pred #(cljstr/includes? % (:substr input)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;;  Interface
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(spec/def ::pred fn?)
(spec/def ::reverse? any?)
(spec/def ::nth number?)
(spec/def ::offset number?)
(spec/def ::substr string?)
(spec/def ::index-of-line-input (spec/keys :opt-un
                                           [::pred
                                            ::reverse?
                                            ::nth
                                            ::substr
                                            ::offset]))

(defn index-of-line [input]
  {:pre [(spec/valid? ::index-of-line-input input)
         (or (:substr input)
             (:pred input))]}
  (let [input (complete-input input)]
    (fn [lines]
      (let [lines ((if (:reverse? input) reverse identity)
                   (numbered-lines lines))
            results (filter (fn [x] ((:pred input) (:line x))) lines)
            i (or (:nth input) 0)]
        (when (empty? results)
          (throw (ex-info "No matching line" {})))
        (when (<= (count results) i)
          (throw (ex-info "Bad" {})))
        (+ (or (:offset input) 0)
           (:index
            (nth results i)))))))

(defn trim-lines [s max-length]
  (map-lines
   s #(trim-line % max-length)))

(defn line-trimmer [max-length]
  #(trim-lines % max-length))

(defn remove-lines-with [sub]
  {:pre [(string? sub)]}
  (fn [s]
    (filter-lines
     s
     (complement #(cljstr/includes? % sub)))))

(defn loc-comment [comment-pref n]
  (if (or (nil? comment-pref)
          (<= n 0))
    []
    [(str
      comment-pref
      " ... "
      n
      " line"
      (if (= n 1) "" "s")
      " of code not shown")]))

(defn line-range
  ([from-index-fn to-index-fn]
   (line-range from-index-fn to-index-fn "//"))
  ([from-index-fn to-index-fn comment-prefix]
   {:pre [(fn? from-index-fn)
          (fn? to-index-fn)]}
   (fn [s]
     (transform-lines
      s
      (fn [lines]
        (let [l (from-index-fn lines)
              u (to-index-fn lines)
              n (count lines)]
          (println "l=" l "u=" u)
          (reduce into
                  []
                  [(loc-comment comment-prefix l)
                   (subvec (vec lines) l u)
                   (loc-comment comment-prefix
                                (- n u))])))))))
