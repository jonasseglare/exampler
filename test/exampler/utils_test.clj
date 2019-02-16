(ns exampler.utils-test
  (:require [clojure.test :refer :all]
            [exampler.utils :refer :all]))

(deftest various-tests
  (is (= (trim-lines "asdfadsf\nasdfsafdasdfasfd" 10)
         "asdfadsf\nasdfsa ..."))
  (is (= 1
         ((index-of-line
           {:reverse? true :pred (constantly true)})
          ["a" "b"])))
  (is (= 0
         ((index-of-line
           {:reverse? false
            :pred (constantly true)})
          ["a" "b"])))
  (is (= 1
         ((index-of-line
           {:reverse? false :pred empty?})
          ["a" "" "b"])))
  (is (= 2
         ((index-of-line
           {:reverse? false :pred empty?})
          ["a" "sdf" "" "b"])))
  (is (= 3
         ((index-of-line
           {:reverse? false :pred empty?}) 
          ["a" "b" "b" "" "c" "" ])))
  (is (= 5
         ((index-of-line
           {:reverse? false
            :pred empty?
            :nth 1}) 
          ["a" "b" "b" "" "c" "" ])))
  (is (= 7
         ((index-of-line
           {:reverse? false
            :pred empty?
            :nth 1
            :offset 2}) 
          ["a" "b" "b" "" "c" "" ]))))
