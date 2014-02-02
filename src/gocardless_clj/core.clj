(ns gocardless-clj.core
  (:require [gocardless-clj.http :as http]
            [gocardless-clj.resources :refer :all]
            [gocardless-clj.protocols :refer :all]))

(defn make-account
  [account-details]
  (fn
    ([f] (f account-details))
    ([f id] (f id account-details))
    ([f k v & kvs]
       (let [args (into {k v} (apply hash-map kvs))]
         (f args account-details)))))

(defn details
  [account]
  (-> (http/path "merchants" (:merchant-id account))
      (http/api-get account)))

(defmulti customers (fn [arg & _] (class arg)))
(defmethod customers
  java.lang.String
  [id account]
     (http/api-get (http/path "users" id) account))
(defmethod customers
  clojure.lang.IPersistentMap
  ([account]
     (-> (http/path "merchants" (:merchant-id account) "users")
         (http/api-get account)))
  ([params account] "Params"))

(defmulti payouts (fn [arg & _] (class arg)))
(defmethod payouts
  java.lang.String
  [id account]
  (http/api-get account (http/path "payouts" id)))
(defmethod payouts
  clojure.lang.IPersistentMap
  ([account]
     (-> (http/path "merchants" (:merchant-id account) "payouts")
         (http/api-get account)))
  ([params account] "Params"))

(defmulti bills (fn [arg & _] (class arg)))
(defmethod bills
  java.lang.String
  [id account]
  (map->Bill (http/api-get account (http/path "bills" id))))
(defmethod bills
  clojure.lang.IPersistentMap
  ([account]
     (let [bills (-> (http/path "merchants" (:merchant-id account) "bills")
                     (http/api-get account))]
       (map map->Bill bills)))
  ([params account] "Params"))

(defmulti subscriptions (fn [arg & _] (class arg)))
(defmethod subscriptions
  java.lang.String
  [account id]
  (-> (http/api-get account (http/path "subscriptions" id))
      (map->Subscription)))
(defmethod subscriptions
  clojure.lang.IPersistentMap
  ([account]
     (let [subscriptions  (-> (http/path "merchants" (:merchant-id account) "subscriptions")
                              (http/api-get account))]
       (map map->Subscription subscriptions)))
  ([params account] "Params"))

(defmulti pre-authorizations (fn [arg & _] (class arg)))
(defmethod pre-authorizations
  java.lang.String
  [account id]
  (-> (http/api-get account (http/path "pre_authorizations" id))
      map->PreAuthorization))
(defmethod pre-authorizations
  clojure.lang.IPersistentMap
  ([account]
     (let [pre-auths (-> (http/path "merchants" (:merchant-id account) "pre_authorizations")
                         (http/api-get account))]
       (map map->PreAuthorization pre-auths)))
  ([params account]))

(defn new-bill
  [{:keys [amount] :as opts} account])

(defn new-subscription
  [{:keys [amount interval-length interval-unit] :as opts} account])

(defn new-pre-authorization
  [{:keys [amount interval-length interval-unit] :as opts} account])

(defn confirm
  [resource account])
