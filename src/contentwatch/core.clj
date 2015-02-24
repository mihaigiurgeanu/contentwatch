(ns contentwatch.core
  (:use httpcrawler.core
        [clojure.java.io :only [reader]])
  (:gen-class))

(def ^{:doc "Configuration file name" :dynamic true}
  *configuration-file-name*
  "contentwatch.conf")

(defn- read-configuration
  "Reads configuration data from the default cofing-file location"
  []
  (with-open [configs (java.io.PushbackReader. (reader *configuration-file-name*))]
    (read configs)))

(defn- create-session [session-file-name]
  (let [last-session-id (with-open [session-file (java.io.PushbackReader. (reader session-file-name))]
                          (read session-file))
        last-session-data (if last-session-id
                            (with-open [data (java.io.PushbackReader. (reader last-session-id))]
                              {:last-session-id last-session-id
                               :pevious-session-id (read data)
                               :last-session-data (read data)
                               })
                            {:previous-session-id nil
                             :last-session-id nil
                             :last-session-data {}})]))

(defn -main
  [& args]
  (let [{:keys [http-options batch-size pages-file session-file]} (read-configuration)
        {:keys [last-session-id last-session-data]} (create-session session-file)]
    (with-open [sites (reader pages-file)]
      (doseq [site (line-seq sites)]
        (crawl site
             (fn [url {:keys [status error body opts]}]
               (if (contains? last-session-data url)
                 nil
                 (println (str "\"NEW\",\"" url "\""))))
             batch-size http-options)))))
