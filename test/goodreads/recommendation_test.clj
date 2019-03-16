(ns goodreads.recommendation-test
  (:require [clojure.data.xml :as dx]
            [clojure.test :as t]
            [clojure.test.check :as tc]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [goodreads.api :refer [collect-similar-books collect-user-books]]
            [goodreads.core :refer [build-recommendations]]
            [goodreads.parsers.book-page :refer [filter-by-shelf]]
            [manifold.deferred :as d]))


;; Here we wanna generate some books list
(defn generate-books [min-books max-books]
  (let [title-gen  (->> gen/string-alphanumeric
                        (gen/such-that #(< 0 (count %))))
        link #(str "http://goodreads.com/" % ".html")
        authors-gen (->> (gen/elements ["Joan Roling" "Chuk Palanik" "A. Pushkin"])
                         (gen/set)
                         (gen/such-that #(< 0 (count %)))
                         (gen/fmap #(map (fn [a] {:name a}) %)))
        shelves-gen (->> (gen/elements ["unknown" "read" "currently-reading" "to-read"])
                         (#(gen/set % {:min-elements 1 :max-elements 1})))]
    (->>
     (gen/hash-map
      :title title-gen
      :authors authors-gen
      :shelves shelves-gen
      :average_rating (gen/double* {:NaN? false
                                    :infinite? false
                                    :min 0.0
                                    :max 5.0}))
     (#(gen/vector % min-books max-books))
     (gen/fmap #(map (fn [i book] (into book {:id i :link (link i)})) (range) %)))))


;; Properies will be:
;; - check count of recommended books
;; - check max and min ratings of recommended books
;; - result is sorted in desc order
(defspec test-prop-of-recommendation-algo
  (prop/for-all
   [n gen/nat
    all-books (generate-books 1 50)]
   (with-redefs-fn {#'goodreads.api/collect-user-books
                    (fn [user-id token] (d/success-deferred all-books))
                    #'goodreads.api/collect-similar-books
                    (fn [books token] (d/success-deferred all-books))}
     (fn [] (let [books-read ((filter-by-shelf "read") all-books)
                  books-reading ((filter-by-shelf "currently-reading") all-books)
                  recommended-books @(build-recommendations {:n n :token "token" :user-id "1"})
                  recommendable-books-ratings (->> all-books
                                                   (filter #(-> % :shelves
                                                                (clojure.set/intersection
                                                                 #{"read" "currently-reading"})
                                                                empty?))
                                                   (map :average_rating))
                  recommended-books-ratings (map :average_rating recommended-books)]
              (t/is
               (or
                (or (empty? recommendable-books-ratings)
                    (empty? recommended-books-ratings))
                (and
                 (= (apply max recommendable-books-ratings)
                    (apply max recommended-books-ratings))
                 (<= (apply min recommendable-books-ratings)
                     (apply min recommended-books-ratings)))))
              (t/is (= (count recommended-books)
                       (min n
                            (- (count all-books)
                               (count books-reading)
                               (count books-read)))))
              (t/is (= (map :average_rating recommended-books)
                       (->> recommended-books (map :average_rating) sort reverse))))))))


(t/deftest test-recommendation-algo
  (with-redefs-fn {#'goodreads.api/collect-user-books
                   (fn [user-id token] (d/success-deferred
                                        [{:id "1"
                                          :title "Harry Potter Part 1"
                                          :link "http://goodreads.com/1.html"
                                          :authors [{:name "Joan Roling"}]
                                          :shelves #{"currently-reading"}}
                                         {:id "2"
                                          :title "Harry Potter Part 2"
                                          :link "http://goodreads.com/2.html"
                                          :authors [{:name "Joan Roling"}]
                                          :shelves #{"read"}}
                                         ]))
                   #'goodreads.api/collect-similar-books
                   (fn [books token] (d/success-deferred
                                      [{:id "2"
                                        :title "Harry Potter Part 2"
                                        :link "http://goodreads.com/2.html"
                                        :authors [{:name "Joan Roling"}]
                                        :average_rating 4.8}
                                       {:id "3"
                                        :title "Harry Potter Part 3"
                                        :link "http://goodreads.com/3.html"
                                        :authors [{:name "Joan Roling"}]
                                        :average_rating 4.5}
                                       {:id "4"
                                        :title "Harry Potter Part 4"
                                        :link "http://goodreads.com/4.html"
                                        :authors [{:name "Joan Roling"}]
                                        :average_rating 4.4}]))}
    #(t/is (= [{:id "3"
                :title "Harry Potter Part 3"
                :link "http://goodreads.com/3.html"
                :authors [{:name "Joan Roling"}]
                :average_rating 4.5}
               {:id "4"
                :title "Harry Potter Part 4"
                :link "http://goodreads.com/4.html"
                :authors [{:name "Joan Roling"}]
                :average_rating 4.4}]
              @(build-recommendations {:user-id "1" :n 2 :token "token"})))))

  (t/deftest test-collect-similar-books
    (with-redefs-fn {#'goodreads.api/api-call
                     (fn [url] (-> (slurp "test/resources/book-page.xml")
                                   dx/parse-str
                                   clojure.zip/xml-zip
                                   d/success-deferred))}
      #(t/is (= [{:id "136251",
                  :title "Harry Potter and the Deathly Hallows (Harry Potter, #7)",
                  :link
                  "https://www.goodreads.com/book/show/136251.Harry_Potter_and_the_Deathly_Hallows",
                  :average_rating 3.0,
                  :authors '({:name "J.K. Rowling"})}
                 {:id "33574273",
                  :title "A Wrinkle in Time (Time Quintet, #1)",
                  :link
                  "https://www.goodreads.com/book/show/33574273-a-wrinkle-in-time",
                  :average_rating 3.0,
                  :authors '({:name "Madeleine L'Engle"})}
                 {:id "7624",
                  :title "Lord of the Flies",
                  :link "https://www.goodreads.com/book/show/7624.Lord_of_the_Flies",
                  :average_rating 3.0,
                  :authors '({:name "William Golding"})}]
                @(collect-similar-books [{:id "50"
                                          :title "Hatchet"
                                          :link "http://goodreads.com/50.xml"
                                          :authors [{:name "Gary Paulsen"}]}] "token")))))

  (t/deftest test-collect-user-books
    (with-redefs-fn {#'goodreads.api/api-call
                     (fn [url] (-> (slurp "test/resources/review-list.xml")
                                   dx/parse-str
                                   clojure.zip/xml-zip
                                   d/success-deferred))}
      #(t/is (= [{:id "5",
                  :title "Harry Potter and the Prisoner of Azkaban (Harry Potter, #3)",
                  :link
                  "https://www.goodreads.com/book/show/5.Harry_Potter_and_the_Prisoner_of_Azkaban",
                  :authors '({:name "J.K. Rowling"}),
                  :shelves #{"read"}}
                 {:id "15881",
                  :title "Harry Potter and the Chamber of Secrets (Harry Potter, #2)",
                  :link
                  "https://www.goodreads.com/book/show/15881.Harry_Potter_and_the_Chamber_of_Secrets",
                  :authors '({:name "J.K. Rowling"}),
                  :shelves #{"read"}}
                 {:id "3",
                  :title "Harry Potter and the Sorcerer's Stone (Harry Potter, #1)",
                  :link
                  "https://www.goodreads.com/book/show/3.Harry_Potter_and_the_Sorcerer_s_Stone",
                  :authors '({:name "J.K. Rowling"}),
                  :shelves #{"read"}}]
                @(collect-user-books 10 "token")))))
