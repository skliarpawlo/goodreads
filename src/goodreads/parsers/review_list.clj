(ns goodreads.parsers.review-list
  (:require [clojure.data.zip.xml :refer [xml-> xml1->]]))


;; review list https://www.goodreads.com/review/list/1.xml


(defn -tag-content [tag]
  (-> tag first :content first))

(defn get-authors-info [review]
  (let [authors (xml1-> review :book :authors :author :name)]
    (->> authors
         (map #(-> % :content first))
         (filter (complement nil?))
         (map #(hash-map :name %)))))

(defn get-shelves [review]
  (->> (xml-> review :shelves :shelf)
       (reduce into)
       (map #(get-in % [:attrs :name]))
       (filter (complement nil?))
       set))

(defn get-book-info [review]
  {:id (-> (xml1-> review :book :id) -tag-content)
   :title (-> (xml1-> review :book :title) -tag-content)
   :link (-> (xml1-> review :book :link) -tag-content)
   :authors (get-authors-info review)
   :shelves (get-shelves review)})

(defn get-reviews [review-list-response]
  (xml-> review-list-response
         :GoodreadsResponse
         :reviews
         :review))

(defn books-info [review-list-response]
  (let [all-reviews (get-reviews review-list-response)]
    (map get-book-info all-reviews)))

(defn reviews-url [user-id key]
  (str "https://www.goodreads.com/review/list/"
       user-id
       ".xml?v=2&key="
       key))
