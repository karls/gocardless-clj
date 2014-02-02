(ns gocardless-clj.t-core
  (:use midje.sweet)
  (:use gocardless-clj.core)
  (:use clj-http.fake)
  (:require [cheshire.core :as json]))

; Dummy account
(def account {:environment :sandbox
              :access-token "TOKEN"
              :merchant-id "123"})

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
    (account-fn dummy) => "Index, 123"
    (account-fn dummy "id") => "Show, 123"
    (account-fn dummy :key "val") => "Index with params, 123"))

(fact "(details) queries merchant's endpoint"
  (with-fake-routes-in-isolation
    {
     "https://sandbox.gocardless.com/api/v1/merchants/123"
     (fn [request] {:status 200
                    :headers {"Content-Type" "application/json"}
                    :body (json/generate-string {:id "123"})})}
    (details account) => {:id "123"}))

(facts "(customers)"
  (with-fake-routes-in-isolation
    {
     "https://sandbox.gocardless.com/api/v1/merchants/123/users"
     (fn [request] {:status 200
                    :headers {"Content-Type" "application/json"}
                    :body (json/generate-string [{:id "C1"} {:id "C2"}])})
     "https://sandbox.gocardless.com/api/v1/users/C1"
     (fn [request] {:status 200
                    :headers {"Content-Type" "application/json"}
                    :body (json/generate-string {:id "C1"})})
     "https://sandbox.gocardless.com/api/v1/merchants/123/users?per_page=10"
     (fn [request] {:status 200
                    :headers {"Content-Type" "application/json"}
                    :body (json/generate-string [{:id "C1"} {:id "C2"}])})}
    (customers account) => [{:id "C1"} {:id "C2"}]
    (customers "C1" account) => {:id "C1"}
    (customers {:key "val"} account) => "Params"))
