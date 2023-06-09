(ns contentwatch.core
  (:use httpcrawler.core
        [clojure.java.io :only [reader writer]]
        #_[pandect.algo.sha256 :only [sha256]]
        [digest :only [sha-256] :rename {sha-256 sha256}])
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
                          (read session-file))]
    (if last-session-id
      (with-open [data (java.io.PushbackReader. (reader last-session-id))]
        (println "last-session-id found: " last-session-id)
        {:last-session-id last-session-id
         :last-session-date (read data)
         :pevious-session-id (read data)
         :last-session-data (read data)
         })
      (do
        (println "last-session-id not found")
        {:previous-session-id nil
         :last-session-id nil
         :last-session-data {}}))))

(defn- log-csv [url status]
  (println (str "\"" url "\",\"" status "\"")))

(defn- register-page [new-session-data url status body-digest]
  (log-csv url status)
  (assoc-in new-session-data [url] body-digest))

(defn- just-log-page [new-session-data url status]
  (log-csv url status)
  new-session-data)

(defn- make-session-id [data session-date last-session-id]
  (sha256 (str session-date last-session-id (pr-str data))))

(defn- save-session [data last-session-id session-file-name]
  (let [session-date (.toString (java.util.Date.))
        session-id (make-session-id data session-date last-session-id)]
    (with-open [session-writer (writer session-id)]
      (binding [*out* session-writer]
        (pr session-date last-session-id data)))
    (with-open [session-writer (writer session-file-name)]
      (binding [*out* session-writer]
        (pr session-id)))))

(defn -main
  [& args]
  (let [{:keys [http-options batch-size pages-file session-file]} (read-configuration)
        {:keys [last-session-id last-session-data]} (create-session session-file)
        new-session-data (agent {})]
    (with-open [sites (reader pages-file)]
      (doseq [site (line-seq sites)]
        (crawl
         site
         (fn [url {:keys [status error body opts]}]
           (if
             error (send new-session-data  just-log-page url "ERROR")
             (let [body-digest (sha256 body)]
               (if (contains? last-session-data url)
                 (if (= body-digest (last-session-data url))
                   (send new-session-data register-page url "SAME" body-digest)
                   (send new-session-data register-page url "CHANGED" body-digest))
               (send new-session-data register-page url "NEW" body-digest)))))
         batch-size http-options)))
    (await new-session-data)
      (doseq [old-url (keys last-session-data)]
        (when-not (contains? @new-session-data old-url)
          (log-csv old-url "DELETED")))
    (save-session @new-session-data last-session-id session-file))
  (shutdown-agents))
