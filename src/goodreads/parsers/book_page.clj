(ns goodreads.parsers.book-page
  (:require [aleph.http :as http]
            [clojure.data.xml :as dx]
            [clojure.data.zip.xml :refer [xml-> xml1->]]
            [manifold.deferred :as d]))
;; book page ;; https://www.goodreads.com/book/show/50.xml

(defn -tag-content [tag]
  (-> tag first :content first))

(defn get-authors-info [review]
  (let [authors (xml1-> review :authors :author :name)]
    (->> authors
         (map #(-> % :content first))
         (filter (complement nil?))
         (map #(hash-map :name %)))))

(defn get-similar-book-info [similar-book]
  {:id (-> (xml1-> similar-book :id) -tag-content)
   :title (-> (xml1-> similar-book :title) -tag-content)
   :link (-> (xml1-> similar-book :link) -tag-content)
   :average_rating (-> (xml1-> similar-book :average_rating) -tag-content Float/parseFloat)
   :authors (get-authors-info similar-book)})

(defn get-similar-books [response] (xml-> response
                                          :GoodreadsResponse
                                          :book
                                          :similar_books
                                          :book))

(defn book-url [book-id key]
  (str "https://www.goodreads.com/book/show/"
       book-id
       ".xml?key="
       key))

(defn filter-by-shelf [shelf]
  (fn [books] (filter #(-> % :shelves (contains? shelf)) books)))
