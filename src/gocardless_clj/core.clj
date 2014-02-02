(ns gocardless-clj.core
  (:require [gocardless-clj.http :as http]
            [gocardless-clj.resources :refer :all]
            [gocardless-clj.protocols :refer :all]))

(defn- dispatch-fn
  "The dispatch function for resource functions."
  [arg & _]
  (class arg))

(defn make-account
  "Create an `account` function based on account details.

  Returns a function that closes over account details and takes either a
  function, a function and an ID or a function and some key-value pairs.

  The argument function can be any of the supported resource-functions:
  customers, payouts, bills, subscriptions or pre-authorizations."
  [account-details]
  (fn
    ([f] (f account-details))
    ([f id] (f id account-details))
    ([f k v & kvs]
       (let [args (into {k v} (apply hash-map kvs))]
         (f args account-details)))))

(defn details
  "Get the account details."
  [account]
  (-> (http/path "merchants" (:merchant-id account))
      (http/api-get account)))

(defmulti customers
  "Retrieve merchant's customers or a single customer.

  Takes as arguments either the account-map, an ID of a customer and the
  account-map, or a params-map and the account-map.

  This is normally passed into the function returned from `make-account`."
  dispatch-fn)
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

(defmulti payouts
  "Retrieve merchant's payouts or a single payout.

  Takes as arguments either the account-map, an ID of a payout and the
  account-map, or a params-map and the account-map.

  This is normally passed into the function returned from `make-account`."
  dispatch-fn)
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

(defmulti bills
  "Retrieve merchant's bills or a single bill.

  Takes as arguments either the account-map, an ID of a bill and the
  account-map, or a params-map and the account-map.

  This is normally passed into the function returned from `make-account`."
  dispatch-fn)
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

(defmulti subscriptions
  "Retrieve merchant's subscriptions or a single subscription.

  Takes as arguments either the account-map, an ID of a subscription and the
  account-map, or a params-map and the account-map.

  This is normally passed into the function returned from `make-account`."
  dispatch-fn)
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

(defmulti pre-authorizations
  "Retrieve merchant's pre-authorizations or a single pre-authorization.

  Takes as arguments either the account-map, an ID of a pre-authorization and the
  account-map, or a params-map and the account-map.

  This is normally passed into the function returned from `make-account`."
  dispatch-fn)
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
