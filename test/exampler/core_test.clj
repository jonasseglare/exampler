(ns exampler.core-test
  (:require [exampler.core :refer :all :as exampler]
            [clojure.test :refer :all]
            [clojure.spec.alpha :as spec]))

(deftest extract-test
  (let [result (extract "./test/exampler/sample.clj")
        samples (:samples result)
        [a b] samples]
    (is (= 2 (count samples)))
    (is (= "Define square\n" (:output a)))
    (is (= "The result is 9\n" (:output b)))
    (is (= 9 (:value b)))
    (is (= 3 (count (:code b))))

    ;; Because we had the idea of annotating lines...
    #_(is (= :result (-> b
                       :code
                       last
                       :annot)))))

(deftest annot-parsing
  (is (= {:code " katt   "}
         (parse-annotated-line " katt   ")))
  (is (= (parse-annotated-line "   asdfa ;; [1]")
         {:code "   asdfa ", :annot 1})))

(deftest line-parsing
  (is (= (spec/conform ::exampler/file ["a"])
         {:init ["a"]}))
  (is (= (spec/conform ::exampler/file ["a" "b" "c"])
         {:init ["a" "b" "c"]}))
  (is (= {:blocks [{:block {:begin "[:begin-example]",
                            :end "[:end-example]"}}]}
         (spec/conform ::exampler/file [
                                        "[:begin-example]"
                                        "[:end-example]"
                                        ])))
  (is (= {:init ["a"], :blocks
          [{:block {:begin "[:begin-example]",
                    :data ["b"],
                    :end "[:end-example]"},
            :space ["c"]}]}
         (spec/conform ::exampler/file ["a"
                                        "[:begin-example]"
                                        "b"
                                        "[:end-example]"
                                        "c"
                                        ]))))

(deftest parse-line-str-test
  (is (= [{:begin "[:begin-example]",
           :data '({:code "b"}),
           :end "[:end-example]"}]
         (parse-file-str (str "a\n"
                              "[:begin-example]\n"
                              "b\n"
                              "[:end-example]\n"
                              "c"))))
  (is (= [{:begin "[:begin-example]",
           :data '({:code "b"}
                   {:code "c" :annot 119}),
           :end "[:end-example]"}]
         (parse-file-str (str "a\n"
                              "[:begin-example]\n"
                              "b\n"
                              "c;; [119]\n"
                              "[:end-example]\n"
                              "c")))))
