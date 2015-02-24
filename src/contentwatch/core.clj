(ns contentwatch.core
  (:use httpcrawler.core)
  (:gen-class))

(defn -main
  [& args]
  (crawl "http://www.agriculture.gov.au/"
         (fn [url {:keys [status error body opts]}]
           (println url status error))
         20
         {}))
