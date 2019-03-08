## Clojure Test Assignment “Books recommendations”

Sample project only. Feel free to change/modify the code given.

# Phase #1

Build books recommendation tool for Goodreads users following the next simple algorithm:
for a given user find all books marked as “read” and choose top 10 by average rating from “similar”
books excluding books that user is currently reading. Use “similarity” definition provided by
Goodreads API (each book has assigned list of "similar books", check any book details to find out more).

# Phase #2 (optional)

Suggest how to improve quality of your recommendations, i.e. using information on authors, topics,
shelves, friends activity etc.

1. Simple improvements
 - Top books of the same author
 - Top books on the same topic
 - Analyze books which are planned to read

2. Recommender systems theory:
 - Collabirative filtration: what do friends of mine read, who have good intersection of interests (on books, topics, etc.)
 - Figure out similar profiles using cosine distance of books read
 - Propose books out of intersection

3. Cosine distance between books:
 - Features could be:
   - genre
   - rating
   - page count
   - age category
   - use shelves counts as feature vector
   - etc
Now we have a book represented as a vector, so we can get look for other books closer by cosine distance.

4. Measurements:
 - collect data about how current recommender system works in real world. Apply A/B testing to be able to compare how each of them performs.


## Links

* [Documentation](https://www.goodreads.com/api/index)

* Rate limits [policy](https://www.goodreads.com/topic/show/17540788-what-s-rate-limit-of-your-api#comment_141992829)

* [List of books](https://www.goodreads.com/api/index#reviews.list)

* [Info on each book](https://www.goodreads.com/api/index#book.show) (you can find similar books and ratings here)

## Usage

Run using `lein`:

```shell
    $ lein run <TOKEN> -u <USER-ID> -n 10 -t 6000
```

Compile and run from JAR:

```shell
    $ lein uberjar
    $ java -jar target/uberjar/goodreads-0.1.0-SNAPSHOT-standalone.jar <TOKEN> [<OPTIONS>]
```

## Test

```shell
    $ lein test
```

## License

Proprietary.

Copyright © 2019
