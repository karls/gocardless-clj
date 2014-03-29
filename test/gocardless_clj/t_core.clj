(ns gocardless-clj.t-core
  (:use midje.sweet)
  (:use gocardless-clj.core)
  (:use clj-http.fake)
  (:require [cheshire.core :as json]
            [gocardless-clj.resources :refer [map->Bill]]))

; Dummy account
(def account {:environment :sandbox
              :merchant-id "MERCHANT1"
              :app-id "APPID"
              :app-secret "APPSECRET"
              :access-token "ACCESSTOKEN"})

(fact "(details) queries merchant's endpoint"
  (with-fake-routes-in-isolation
    {
     "https://sandbox.gocardless.com/api/v1/merchants/MERCHANT1"
     (fn [request] {:status 200
                    :headers {"Content-Type" "application/json"}
                    :body (json/generate-string {:id "123"})})}
    (details account) => {:id "123"}))

(facts "(customers)"
  (with-fake-routes-in-isolation
    {
     "https://sandbox.gocardless.com/api/v1/merchants/MERCHANT1/users"
     (fn [request] {:status 200
                    :headers {"Content-Type" "application/json"}
                    :body (json/generate-string [{:id "C1"} {:id "C2"}])})
     "https://sandbox.gocardless.com/api/v1/users/C1"
     (fn [request] {:status 200
                    :headers {"Content-Type" "application/json"}
                    :body (json/generate-string {:id "C1"})})
     "https://sandbox.gocardless.com/api/v1/merchants/MERCHANT1/users?per_page=10"
     (fn [request] {:status 200
                    :headers {"Content-Type" "application/json"}
                    :body (json/generate-string [{:id "C1"} {:id "C2"}])})}
    (customers account) => [{:id "C1"} {:id "C2"}]
    (customers account {:id "C1"}) => {:id "C1"}
    (customers account {:per_page 10}) => [{:id "C1"} {:id "C2"}]))

(facts "(new-bill)"
  (def limit new-bill)

  (limit account {:amount "foo"}) => (throws AssertionError)
  (limit account {:amount 10.0}) => #"https://sandbox.gocardless.com"
  (limit account {:amount 10.0}) => #"connect/bills/new"
  (limit account {:amount 10.0}) => #"signature="
  (limit account {:amount 10.0}) => #"client_id=APPID"
  (limit account {:amount 10.0}) => #"timestamp="
  (limit account {:amount 10.0}) => #"nonce="
  (limit account {:amount 10.0}) => #"bill%5Bamount%5D=10.0"
  (limit account {:amount 10.0}) => #"bill%5Bmerchant_id%5D=MERCHANT1")

(facts "(new-subscription)"
  (def limit new-subscription)

  (limit account {:amount "foo" :interval_length 1 :interval_unit "day"})
  => (throws AssertionError)

  (limit account {:amount 10.0 :interval_length 1})
  => (throws AssertionError)

  (limit account {:amount 10.0 :interval_length -9 :interval_unit "day"})
  => (throws AssertionError)

  (limit account {:amount 10.0 :interval_length 3 :interval_unit "year"})
  => (throws AssertionError)

  (let [valid-params {:amount 10.0
                      :interval_length 3
                      :interval_unit "month"}]
    (limit account valid-params) => #"https://sandbox.gocardless.com"
    (limit account valid-params) => #"connect/subscriptions/new"
    (limit account valid-params) => #"signature="
    (limit account valid-params) => #"client_id=APPID"
    (limit account valid-params) => #"timestamp="
    (limit account valid-params) => #"nonce="
    (limit account valid-params) => #"subscription%5Bamount%5D=10.0"
    (limit account valid-params) => #"subscription%5Bmerchant_id%5D=MERCHANT1"))

(facts "(new-pre-authorization)"
  (def limit new-pre-authorization)

  (limit account {:max_amount "foo" :interval_length 1 :interval_unit "day"})
  => (throws AssertionError)

  (limit account {:max_amount 10.0 :interval_length 1})
  => (throws AssertionError)

  (limit account {:max_amount 10.0 :interval_length -9 :interval_unit "day"})
  => (throws AssertionError)

  (limit account {:max_amount 10.0 :interval_length 3 :interval_unit "year"})
  => (throws AssertionError)

  (let [valid-params {:max_amount 10.0
                      :interval_length 3
                      :interval_unit "month"}]
    (limit account valid-params) => #"https://sandbox.gocardless.com"
    (limit account valid-params) => #"connect/pre_authorizations/new"
    (limit account valid-params) => #"signature="
    (limit account valid-params) => #"client_id=APPID"
    (limit account valid-params) => #"timestamp="
    (limit account valid-params) => #"nonce="
    (limit account valid-params) => #"pre_authorization%5Bmax_amount%5D=10.0"
    (limit account valid-params) => #"pre_authorization%5Bmerchant_id%5D=MERCHANT1"))

(with-fake-routes-in-isolation
  {
   "https://sandbox.gocardless.com/api/v1/confirm"
   {:post (fn [request] {:status 200
                         :headers {"Content-Type" "application/json"}
                         :body (json/generate-string {:some "data"})})}}


  (let [params {"resource_id" "BILLID"
                "resource_type" "bill"
                "resource_uri" "https://some.uri.com/bills/BILLID"
                "signature" "977aab1a7949b00c2dfab0f7816a113d020d9bcdb9c788d67c673b1ec14681b9"
                "state" "foo"}]
    (facts "(confirm-resource)"
      (confirm-resource account params) => {:some "data"}
      ;; if the request was tampered with and for example the state param
      ;; was removed or changed
      (confirm-resource account (dissoc params "state")) => false
      (confirm-resource account (assoc params "state" "bar")) => false)))

(with-fake-routes-in-isolation
  {
   "https://sandbox.gocardless.com/api/v1/bills"
   {:post (fn [request] {:status 201
                         :headers {"Content-Type" "application/json"}
                         :body (json/generate-string {:id "BILLID"})})}}
  (facts "(create-bill)"
    (create-bill account {:amount 10.0 :pre_authorization_id "PREAUTH1"})
    => (map->Bill {:id "BILLID"})))
