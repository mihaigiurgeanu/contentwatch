(defproject contentwatch "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [httpcrawler "0.1.0-SNAPSHOT"]
                 #_[pandect "0.5.1"]
                 [digest "1.4.4"]]
  :main ^:skip-aot contentwatch.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
