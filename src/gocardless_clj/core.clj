(ns gocardless-clj.core
  (:require [gocardless-clj.client :as client]
            [gocardless-clj.resources :refer :all]
            [gocardless-clj.protocols :refer :all]
            [gocardless-clj.signature :refer [sign-params]]))

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
    ([f arg] (f arg account-details))
    ([f k v & kvs]
       (let [args (into {k v} (apply hash-map kvs))]
         (f args account-details)))))

(defn details
  "Get the account details."
  [account]
  (-> (client/path "merchants" (:merchant-id account))
      (client/api-get {} account)))

(defmulti customers
  "Retrieve merchant's customers or a single customer.

  Takes as arguments either the account-map, an ID of a customer and the
  account-map, or a params-map and the account-map.

  This is normally passed into the function returned from `make-account`."
  dispatch-fn)
(defmethod customers
  java.lang.String
  [id account]
     (client/api-get (client/path "users" id) {} account))
(defmethod customers
  clojure.lang.IPersistentMap
  ([account]
     (-> (client/path "merchants" (:merchant-id account) "users")
         (client/api-get {} account)))
  ([params account]
     (-> (client/path "merchants" (:merchant-id account) "users")
         (client/api-get params account))))

(defmulti payouts
  "Retrieve merchant's payouts or a single payout.

  Takes as arguments either the account-map, an ID of a payout and the
  account-map, or a params-map and the account-map.

  This is normally passed into the function returned from `make-account`."
  dispatch-fn)
(defmethod payouts
  java.lang.String
  [id account]
  (client/api-get (client/path "payouts" id) {} account))
(defmethod payouts
  clojure.lang.IPersistentMap
  ([account]
     (-> (client/path "merchants" (:merchant-id account) "payouts")
         (client/api-get {} account)))
  ([params account]
     (-> (client/path "merchants" (:merchant-id account) "payouts")
         (client/api-get params account))))

(defmulti bills
  "Retrieve merchant's bills or a single bill.

  Takes as arguments either the account-map, an ID of a bill and the
  account-map, or a params-map and the account-map.

  This is normally passed into the function returned from `make-account`."
  dispatch-fn)
(defmethod bills
  java.lang.String
  [id account]
  (-> (client/api-get (client/path "bills" id) {} account)
      map->Bill))
(defmethod bills
  clojure.lang.IPersistentMap
  ([account]
     (let [bills (-> (client/path "merchants" (:merchant-id account) "bills")
                     (client/api-get {} account))]
       (map map->Bill bills)))
  ([params account]
     (-> (client/path "merchants" (:merchant-id account) "bills")
         (client/api-get params account))))

(defmulti subscriptions
  "Retrieve merchant's subscriptions or a single subscription.

  Takes as arguments either the account-map, an ID of a subscription and the
  account-map, or a params-map and the account-map.

  This is normally passed into the function returned from `make-account`."
  dispatch-fn)
(defmethod subscriptions
  java.lang.String
  [account id]
  (-> (client/api-get (client/path "subscriptions" id) {} account)
      (map->Subscription)))
(defmethod subscriptions
  clojure.lang.IPersistentMap
  ([account]
     (let [subscriptions  (-> (client/path "merchants" (:merchant-id account) "subscriptions")
                              (client/api-get {} account))]
       (map map->Subscription subscriptions)))
  ([params account]
     (-> (client/path "merchants" (:merchant-id account) "subscriptions")
         (client/api-get params account))))

(defmulti pre-authorizations
  "Retrieve merchant's pre-authorizations or a single pre-authorization.

  Takes as arguments either the account-map, an ID of a pre-authorization and the
  account-map, or a params-map and the account-map.

  This is normally passed into the function returned from `make-account`."
  dispatch-fn)
(defmethod pre-authorizations
  java.lang.String
  [account id]
  (-> (client/api-get (client/path "pre_authorizations" id) {} account)
      map->PreAuthorization))
(defmethod pre-authorizations
  clojure.lang.IPersistentMap
  ([account]
     (let [preauths (-> (client/path "merchants" (:merchant-id account) {} "pre_authorizations")
                        (client/api-get account))]
       (map map->PreAuthorization preauths)))
  ([params account]
     (-> (client/path "merchants" (:merchant-id account) "pre_authorizations")
         (client/api-get params account))))

(defn new-bill
  "Returns the Connect URL for a new Bill.

  Required map keys: `:amount`."
  [{:keys [amount] :as opts} account]
  {:pre [(number? amount)
         (> amount 1.0)]}
  (let [params (assoc opts :amount (bigdec amount))]
    (client/new-limit-url "bill" params account)))

(defn create-bill
  [{:keys [amount pre_authorization_id] :as opts} account]
  {:pre [(number? amount)
         (> amount 1.0)
         (string? pre_authorization_id)
         (not (empty? pre_authorization_id))]}
  (let [params (assoc opts :amount (bigdec amount))]
    (-> (client/api-post "bills" {"bill" params} account)
        map->Bill)))

(defn new-subscription
  "Returns the Connect URL for a new Subscription.

  Required map keys: `:amount`, `:interval_length`, `:interval_unit`."
  [{:keys [amount interval_length interval_unit] :as opts} account]
  {:pre [(number? amount)
         (number? interval_length)
         (pos? interval_length)
         (contains? #{"day" "week" "month"} interval_unit)]}
  (let [params (assoc opts :amount (bigdec amount))]
    (client/new-limit-url "subscription" params account)))

(defn new-pre-authorization
  "Returns the Connect URL for a new PreAuthorization.

  Required keys: `:max_amount`, `:interval_length`, `:interval_unit`."
  [{:keys [max_amount interval_length interval_unit] :as opts} account]
  {:pre [(number? max_amount)
         (number? interval_length)
         (pos? interval_length)
         (contains? #{"day" "week" "month"} interval_unit)]}
  (let [params (assoc opts :max_amount (bigdec max_amount))]
    (client/new-limit-url "pre_authorization" params account)))

(defn confirm-resource
  "Confirm a created limit (bill/subscription/preauthorization).

  Signature will be checked. If signatures don't match, the resource will not be
  confirmed and `false` is returned.

  `params` is assumed to be a map containing keys and values from the query
  string. Map keys are assumed to be keywords."
  [params account]
  {:pre [(every? (set (keys params))
                 [:resource_id :resource_type :resource_uri :signature])]}
  (let [ks [:resource_id :resource_type :resource_uri :state :signature]
        params (select-keys params ks)
        to-sign (clojure.walk/stringify-keys (dissoc params :signature))
        data (select-keys params [:resource_id :resource_type])]
    (if (= (:signature params) (sign-params to-sign (:app-secret account)))
      (client/api-post "confirm" data account)
      false)))
