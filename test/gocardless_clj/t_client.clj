(ns gocardless-clj.t-client
  (:use midje.sweet)
  (:use gocardless-clj.client)
  (:use clj-http.fake)
  (:require [cheshire.core :as json]))

(def account {:environment :sandbox
              :access-token "TOKEN"})

(fact "(user-agent) string looks at the project version"
  (let [version (System/getProperty "gocardless-clj.version")]
    (#'gocardless-clj.client/ua-string) => (str "gocardless-clj/v" version)))

(fact "(underscorize-keys) recursively replaces dashes in keys with underscores"
  (let [m {"key-1" "val-1" "key-2" {"key-3" "val3"}}]
    (#'gocardless-clj.client/underscorize-keys m)
    => {"key_1" "val-1" "key_2" {"key_3" "val3"}}))

(fact "(path) constructs `/`-joined string"
  (path "foo" "bar" "baz") => "foo/bar/baz")

(facts "about api-url"
  (api-url "http://google.com" account) => "http://google.com"
  (api-url "https://google.com" account) => "https://google.com"
  (api-url "bills/BILL1" {:environment :live}) => "https://gocardless.com/api/v1/bills/BILL1"
  (api-url "bills/BILL1" account) => "https://sandbox.gocardless.com/api/v1/bills/BILL1"
  (api-url "bills/BILL1" {:environment :foo}) => (throws IllegalArgumentException))

(with-fake-routes-in-isolation
  {"https://sandbox.gocardless.com/api/v1/bills/BILL1"
   {:get (fn [request] {:status 200
                        :headers {"Content-Type" "application/json"}
                        :body (json/generate-string {:id "BILL1"})})}
   "https://sandbox.gocardless.com/api/v1/bills/BILL1/retry"
   {:post (fn [request] {:status 200
                         :headers {"Content-Type" "application/json"}
                         :body (json/generate-string {:id "BILL1"})})}
   "https://sandbox.gocardless.com/api/v1/subscriptions/SUBS1/cancel"
   {:put (fn [request] {:status 200
                        :headers {"Content-Type" "application/json"}
                        :body (json/generate-string {:id "SUBS1"})})}
   }

  (facts "(api-get)"
    (api-get "bills/BILL1" {} account) => {:id "BILL1"})

  (facts "(api-post)"
    (api-post "bills/BILL1/retry" {} account) => {:id "BILL1"})

  (facts "(api-put)"
    (api-put "subscriptions/SUBS1/cancel" {} account) => {:id "SUBS1"}))
