(defproject gocardless-clj "0.5.2"
  :description "Clojure client library for the GoCardless API"
  :url "https://github.com/karls/gocardless-clj"
  :license {:name "MIT License"
            :url "http://choosealicense.com/licenses/mit/"}
  :profiles {:dev {:dependencies [[midje "1.8.3" :exclusions [org.clojure/clojure]]
                                  [clj-http-fake "1.0.2"]
                                  [compojure "1.4.0"]
                                  [ring-json-params "0.1.3"]
                                  [javax.servlet/servlet-api "2.5"]]
                   :plugins [[lein-midje "3.2"]
                             [lein-ring "0.9.7"]
                             [lein-marginalia "0.8.0"]]
                   :source-paths ["src" "example"]
                   :ring {:handler example-app/app}}}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [pandect "0.5.4"]
                 [cheshire "5.5.0"]
                 [clj-http "2.0.1"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-time "0.11.0"]])
