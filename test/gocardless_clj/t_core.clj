(ns gocardless-clj.t-core
  (:use midje.sweet)
  (:use gocardless-clj.core)
  (:use clj-http.fake)
  (:require [cheshire.core :as json]))

; Dummy account
(def account {:environment :sandbox
              :merchant-id "MERCHANT1"
              :app-id "APPID"
              :app-secret "APPSECRET"
              :access-token "ACCESSTOKEN"})

;; Set up a dummy function similar to customers/bills/subscriptions/pre-authorizations
;; This precisely mirrors the multimethod definition from gocardless-clj.core
(defmulti dummy (fn [arg & _] (class arg)))
(defmethod dummy
  java.lang.String
  [id account] (format "Show, %s" (:merchant-id account)))
(defmethod dummy
  clojure.lang.IPersistentMap
  ([account] (format "Index, %s" (:merchant-id account)))
  ([params account] (format "Index with params, %s" (:merchant-id account))))

(fact "(make-account) produces a closure over account details "
  (let [account-fn (make-account account)]
    (account-fn dummy) => "Index, MERCHANT1"
    (account-fn dummy "id") => "Show, MERCHANT1"
    (account-fn dummy :key "val") => "Index with params, MERCHANT1"))

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
    (customers "C1" account) => {:id "C1"}
    (customers {:key "val"} account) => "Params"))

(facts "(new-bill)"
  (def limit new-bill)

  (limit {:amount "foo"} account) => (throws AssertionError)
  (limit {:amount 10.0} account) => #"https://sandbox.gocardless.com"
  (limit {:amount 10.0} account) => #"connect/bills/new"
  (limit {:amount 10.0} account) => #"signature="
  (limit {:amount 10.0} account) => #"client_id=APPID"
  (limit {:amount 10.0} account) => #"timestamp="
  (limit {:amount 10.0} account) => #"nonce="
  (limit {:amount 10.0} account) => #"bill%5Bamount%5D=10.0"
  (limit {:amount 10.0} account) => #"bill%5Bmerchant_id%5D=MERCHANT1")

(facts "(new-subscription)"
  (def limit new-subscription)

  (limit {:amount "foo" :interval-length 1 :interval-unit "day"} account)
  => (throws AssertionError)

  (limit {:amount 10.0 :interval-length 1} account)
  => (throws AssertionError)

  (limit {:amount 10.0 :interval-length -9 :interval-unit "day"} account)
  => (throws AssertionError)

  (limit {:amount 10.0 :interval-length 3 :interval-unit "year"} account)
  => (throws AssertionError)

  (let [valid-params {:amount 10.0
                      :interval-length 3
                      :interval-unit "month"}]
    (limit valid-params account) => #"https://sandbox.gocardless.com"
    (limit valid-params account) => #"connect/subscriptions/new"
    (limit valid-params account) => #"signature="
    (limit valid-params account) => #"client_id=APPID"
    (limit valid-params account) => #"timestamp="
    (limit valid-params account) => #"nonce="
    (limit valid-params account) => #"subscription%5Bamount%5D=10.0"
    (limit valid-params account) => #"subscription%5Bmerchant_id%5D=MERCHANT1"))

(facts "(new-pre-authorization)"
  (def limit new-pre-authorization)

  (limit {:max-amount "foo" :interval-length 1 :interval-unit "day"} account)
  => (throws AssertionError)

  (limit {:max-amount 10.0 :interval-length 1} account)
  => (throws AssertionError)

  (limit {:max-amount 10.0 :interval-length -9 :interval-unit "day"} account)
  => (throws AssertionError)

  (limit {:max-amount 10.0 :interval-length 3 :interval-unit "year"} account)
  => (throws AssertionError)

  (let [valid-params {:max-amount 10.0
                      :interval-length 3
                      :interval-unit "month"}]
    (limit valid-params account) => #"https://sandbox.gocardless.com"
    (limit valid-params account) => #"connect/pre_authorizations/new"
    (limit valid-params account) => #"signature="
    (limit valid-params account) => #"client_id=APPID"
    (limit valid-params account) => #"timestamp="
    (limit valid-params account) => #"nonce="
    (limit valid-params account) => #"pre_authorization%5Bmax_amount%5D=10.0"
    (limit valid-params account) => #"pre_authorization%5Bmerchant_id%5D=MERCHANT1"))
