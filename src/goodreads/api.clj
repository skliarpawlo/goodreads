(ns goodreads.api
  (:require [aleph.http :as http]
            [clojure.data.xml :as dx]
            [goodreads.parsers.book-page
             :refer
             [book-url get-similar-book-info get-similar-books]]
            [goodreads.parsers.review-list :refer [books-info reviews-url]]
            [manifold.deferred :as d]
            [manifold.stream :as s]))

(defn api-call [url]
  (d/chain (http/get url)
           :body
           dx/parse
           clojure.zip/xml-zip))

(defn collect-user-books [user-id key]
  (d/chain (api-call (reviews-url user-id key)) books-info))

(defn collect-similar-books [books key]
  (let [api-queue (s/stream)
        api-source (s/throttle 1 api-queue)
        books-urls (->> books (map :id) (map #(book-url % key)))]
    (s/put-all! api-queue books-urls)
    (s/close! api-queue)
    (->> api-source
         (s/map (fn [book-url] (d/chain (api-call book-url)
                                        get-similar-books
                                        #(map get-similar-book-info %))))
         (s/reduce into []))))
