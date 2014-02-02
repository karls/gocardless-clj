(defproject gocardless-clj "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :profiles {:dev {:dependencies [[midje "1.6.0"]
                                  [clj-http-fake "0.7.8"]]}}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [pandect "0.3.0"]
                 [clj-http "0.7.8"]
                 [org.clojure/data.json "0.2.4"]
                 [clj-time "0.6.0"]])
