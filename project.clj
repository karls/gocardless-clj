(defproject gocardless-clj "0.3.1"
  :description "Clojure client library for the GoCardless API"
  :url "https://github.com/karls/gocardless-clj"
  :license {:name "MIT License"
            :url "http://choosealicense.com/licenses/mit/"}
  :profiles {:dev {:dependencies [[midje "1.6.0" :exclusions [org.clojure/clojure]]
                                  [clj-http-fake "0.7.8"]
                                  [compojure "1.1.6"]]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-ring "0.8.10"]]
                   :source-paths ["src" "example"]
                   :ring {:handler example-app/app}}}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [pandect "0.3.0"]
                 [clj-http "0.7.8"]
                 [org.clojure/data.json "0.2.4"]
                 [clj-time "0.6.0"]])
