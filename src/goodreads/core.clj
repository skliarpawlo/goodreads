(ns goodreads.core
  (:gen-class)
  (:require [clojure.tools.cli :as cli]
            [goodreads.parsers.review-list :refer [books-info]]
            [goodreads.parsers.book-page :refer [get-similar-book-info
                                                 get-similar-books
                                                 filter-by-shelf]]
            [goodreads.api :refer [collect-user-books collect-similar-books]]
            [manifold.deferred :as d]
            [manifold.stream :as s]))

(defn build-recommendations [{:keys [user-id n token]}]
  (let [user-books (collect-user-books user-id token)
        books-read (d/chain user-books (filter-by-shelf "read"))
        books-reading (d/chain user-books (filter-by-shelf "currently-reading"))
        similar-books (d/chain books-read
                               #(collect-similar-books % token))
        banned-book-ids (d/chain (d/zip books-read books-reading)
                                 #(apply concat %)
                                 #(map :id %)
                                 set)]
    (d/chain (d/zip similar-books banned-book-ids)
             (fn [[similar-books banned-book-ids]]
               (->> similar-books
                    (filter (complement (fn [banned-books]
                                          (banned-book-ids (:id banned-books)))))
                    (sort-by :average_rating)
                    reverse
                    (take n))))))

(def cli-options [["-t"
                   "--timeout-ms T"
                   "Wait before finished"
                   :default 5000
                   :parse-fn #(Integer/parseInt %)]
                  ["-u"
                   "--user-id USER_ID"
                   "User id to build recommendation for"]
                  ["-n"
                   "--number-books N"
                   "How many books do you want to recommend"
                   :default 10
                   :parse-fn #(Integer/parseInt %)]
                  ["-h" "--help"]])

(defn book->str [{:keys [title link authors]}]
  (format "\"%s\" by %s\nMore: %s"
          title
          (->> authors
               (map :name)
               (clojure.string/join ", "))
          link))

(defn -main [& args]
  (let [{:keys [options errors summary]} (cli/parse-opts args cli-options)]
    (cond
      (contains? options :help) (do (println summary) (System/exit 0))
      (some? errors) (do (println errors) (System/exit 1))
      (empty? args) (do (println "Please, specify user's token") (System/exit 1))
      :else (let [config {:token (first args)
                          :user-id (:user-id options)
                          :n (:number-books options)}
                  books (-> (build-recommendations config)
                            (d/timeout! (:timeout-ms options) ::timeout)
                            deref)]
              (cond
                (= ::timeout books) (println "Not enough time :(")
                (empty? books) (println "Nothing found, leave me alone :(")
                :else (doseq [[i book] (map-indexed vector books)]
                        (println (str "#" (inc i)))
                        (println (book->str book))
                        (println)))))))
