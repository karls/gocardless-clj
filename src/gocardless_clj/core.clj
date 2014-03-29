(ns gocardless-clj.core
  (:require [gocardless-clj.client :as c]
            [gocardless-clj.resources :refer :all]
            [gocardless-clj.signature :refer [sign-params]]))

(def cancel #'gocardless-clj.protocols/cancel)
(def cancelable? #'gocardless-clj.protocols/cancelable?)
(def retry #'gocardless-clj.protocols/retry)
(def retriable? #'gocardless-clj.protocols/retriable?)

(defn- only-id?
  [opts-list]
  (= (count opts-list) 1))

(defn details
  "Get the account details."
  [account]
  (c/api-get account (c/path "merchants" (:merchant-id account)) {}))

(defn customers
  "Retrieve merchant's customers or a single customer.

  Takes as arguments either the account-map and an ID of a customer, or the
  account-map and params."
  [account & opts]
  (if (only-id? opts)
    (c/api-get account (c/path "users" (first opts)) {})
    (c/api-get account
               (c/path "merchants" (:merchant-id account) "users")
               (apply hash-map opts))))

(defn payouts
  "Retrieve merchant's payouts or a single payout.

  Takes as arguments either the account-map and an ID of a payout, or the
  account-map and params."
  [account & opts]
  (if (only-id? opts)
    (c/api-get account (c/path "payouts" (first opts)) {})
    (c/api-get account
               (c/path "merchants" (:merchant-id account) "payouts")
               (apply hash-map opts))))

(defn bills
  "Retrieve merchant's bills or a single bill.

  Takes as arguments either the account-map and an ID of a bill, or the
  account-map and params."
  [account & opts]
  (if (only-id? opts)
    (-> (c/api-get account (c/path "bills" (first opts)) {}) map->Bill)
    (let [path (c/path "merchants" (:merchant-id account) "bills")
          bills (c/api-get account path (apply hash-map opts))]
      (map map->Bill bills))))

(defn subscriptions
  "Retrieve merchant's subscriptions or a single subscription.

  Takes as arguments either the account-map and an ID of a subscription, or the
  account-map and params."
  [account & opts]
  (if (only-id? opts)
    (-> (c/api-get account (c/path "subscriptions" (first opts)) {})
        (map->Subscription))
    (let [path (c/path "merchants" (:merchant-id account) "subscriptions")
          subs (c/api-get account path (apply hash-map opts))]
      (map map->Subscription subs))))

(defn pre-authorizations
  "Retrieve merchant's pre-authorizations or a single pre-authorization.

  Takes as arguments either the account-map and an ID of a preauth, or the
  account-map and params."
  [account & opts]
  (if (only-id? opts)
    (-> (c/api-get account (c/path "pre_authorizations" (first opts)) {})
        map->PreAuthorization)
    (let [path (c/path "merchants" (:merchant-id account) "pre_authorizations")
          preauths (c/api-get account path (apply hash-map opts))]
      (map map->PreAuthorization preauths))))

(defn new-bill
  "Returns the Connect URL for a new Bill.

  Required map keys: `:amount`."
  [account {:keys [amount] :as opts}]
  {:pre [(number? amount)
         (> amount 1.0)]}
  (let [params (assoc opts :amount (bigdec amount))]
    (c/new-limit-url account "bill" params)))

(defn create-bill
  [account {:keys [amount pre_authorization_id] :as opts}]
  {:pre [(number? amount)
         (> amount 1.0)
         (string? pre_authorization_id)
         (not (empty? pre_authorization_id))]}
  (let [params (assoc opts :amount (bigdec amount))]
    (-> (c/api-post account "bills" {"bill" params})
        map->Bill)))

(defn new-subscription
  "Returns the Connect URL for a new Subscription.

  Required map keys: `:amount`, `:interval_length`, `:interval_unit`."
  [account {:keys [amount interval_length interval_unit] :as opts}]
  {:pre [(number? amount)
         (number? interval_length)
         (pos? interval_length)
         (contains? #{"day" "week" "month"} interval_unit)]}
  (let [params (assoc opts :amount (bigdec amount))]
    (c/new-limit-url account "subscription" params)))

(defn new-pre-authorization
  "Returns the Connect URL for a new PreAuthorization.

  Required keys: `:max_amount`, `:interval_length`, `:interval_unit`."
  [account {:keys [max_amount interval_length interval_unit] :as opts}]
  {:pre [(number? max_amount)
         (number? interval_length)
         (pos? interval_length)
         (contains? #{"day" "week" "month"} interval_unit)]}
  (let [params (assoc opts :max_amount (bigdec max_amount))]
    (c/new-limit-url account "pre_authorization" params)))

(defn confirm-resource
  "Confirm a created limit (bill/subscription/preauthorization).

  Signature will be checked. If signatures don't match, the resource will not be
  confirmed and `false` is returned.

  `params` is assumed to be a map containing keys and values from the query
  string. Map keys are assumed to be strings."
  [account params]
  {:pre [(every? (set (keys params))
                 ["resource_id" "resource_type" "resource_uri" "signature"])]}
  (let [ks [:resource_id :resource_type :resource_uri :state :signature]
        params (clojure.walk/keywordize-keys params)
        params (select-keys params ks)
        to-sign (dissoc params :signature)
        data (select-keys params [:resource_id :resource_type])]
    (if (= (:signature params) (sign-params to-sign (:app-secret account)))
      (c/api-post account "confirm" data {:basic-auth [(:app-id account)
                                                       (:app-secret account)]})
      false)))
