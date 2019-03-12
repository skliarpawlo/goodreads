(ns goodreads.recommendation-test
  (:require [clojure.data.xml :as dx]
            [clojure.test :as t]
            [goodreads.api :refer [collect-similar-books]]
            [goodreads.core :refer [build-recommendations]]
            [manifold.deferred :as d]
            [goodreads.api :refer [collect-user-books]]))

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
