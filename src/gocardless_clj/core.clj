;; # Client library for the [GoCardless API](https://developer.gocardless.com/#introduction)
;;
(ns gocardless-clj.core
  (:require [gocardless-clj.client :as c]
            [gocardless-clj.resources :refer :all]
            [gocardless-clj.signature :refer [sign-params]]))

(def cancel      #'gocardless-clj.protocols/cancel)
(def cancelable? #'gocardless-clj.protocols/cancelable?)
(def retry       #'gocardless-clj.protocols/retry)
(def retriable?  #'gocardless-clj.protocols/retriable?)

;; ## Fetch details for the authenticating merchant
;;
(defn details
  "Get the account details.

  Example:

    (details account)"
  [account]
  (c/api-get account (c/path "merchants" (:merchant-id account)) {}))

;; ## Core resource lookup functions
;;
;; Every resource has a singular and a plural version. For example the customer
;; function comes in two variants - `customer` and `customers`. The singular
;; variant takes an ID of the resource and fetches the resource. The plural
;; is capable of fetching a single resource, or a collection. The singular
;; version is just a convenience.
;;
;; The plural version takes a params map as the second argument. The map can
;; contain any keys outlined in [Filtering](https://developer.gocardless.com/#filtering)
;; and [Pagination](https://developer.gocardless.com/#pagination). The map
;; can be omitted or left empty, in which case the defaults are used.
;;
(defn customer
  "Retrieve a single customer by their ID.

  Example:

    (customer account \"0K636ZDWM9\")"
  [account id]
  (customers account {:id id}))

(defn customers
  "Retrieve merchant's customers or a single customer.

  Takes as arguments either the account-map and no other params, or the
  account-map and a map of params.

  Examples:

    (customers account)
    (customers account {:id \"0K636ZDWM9\"})
    (customers account {:per_page 5 :page 2})"
  ([account] (customers account {}))
  ([account {:keys [id] :as params}]
     (if id
       (c/api-get account (c/path "users" id) {})
       (c/api-get account
                  (c/path "merchants" (:merchant-id account) "users")
                  params))))

(defn payout
  "Retrieve a single payout by the payout ID.

  Example:

    (payout account \"0K636ZDWM9\")"
  [account id]
  (c/api-get account (c/path "payouts" id) {}))
(defn payouts
  "Retrieve merchant's payouts or a single payout.

  Takes as arguments either the account-map and no other params, or the
  account-map and a map of params.

  Examples:

    (payouts account)
    (payouts account {:id \"0K636ZDWM9\"})
    (payouts account {:per_page 5 :page 2})"
  ([account] (payouts account {}))
  ([account {:keys [id] :as params}]
     (if id
       (c/api-get account (c/path "payouts" id) {})
       (c/api-get account
                  (c/path "merchants" (:merchant-id account) "payouts")
                  params))))

(defn bill
  "Retrieve a single bill by the bill ID.

  Example:

    (bill account \"0K636ZDWM9\")"
  [account id]
  (map->Bill (c/api-get account (c/path "bills" id) {}) map->Bill))

(defn bills
  "Retrieve merchant's bills or a single bill.

  Takes as arguments either the account-map and an ID of a bill, or the
  account-map and params.

  Examples:

    (bills account)
    (bills account {:id \"0K636ZDWM9\"})
    (bills account {:per_page 5 :page 2})"
  ([account] (bills account {}))
  ([account {:keys [id] :as params}]
     (if id
       (-> (c/api-get account (c/path "bills" id) {}) map->Bill)
       (let [path (c/path "merchants" (:merchant-id account) "bills")
             bills (c/api-get account path params)]
         (map map->Bill bills)))))

(defn subscription
  "Retrieve a single subscription by the subscription ID.

  Example:

    (subscription account \"0K636ZDWM9\")"
  [account id]
  (map->Subscription (c/api-get account (c/path "subscriptions" id) {})))

(defn subscriptions
  "Retrieve merchant's subscriptions or a single subscription.

  Takes as arguments either the account-map and no other arguments, or the
  account-map and a map of params.

  Examples:

    (subscriptions account)
    (subscriptions account {:id \"0K636ZDWM9\"})
    (subscriptions account {:per_page 5 :page 2})"
  ([account] (subscriptions account {}))
  ([account {:keys [id] :as params}]
     (if id
       (-> (c/api-get account (c/path "subscriptions" id) {})
           (map->Subscription))
       (let [path (c/path "merchants" (:merchant-id account) "subscriptions")
             subs (c/api-get account path params)]
         (map map->Subscription subs)))))

(defn pre-authorization
  "Retrieve a single pre-authorization by the pre-authorization ID.

  Example:

    (pre-authorization account \"0K636ZDWM9\")"
  [account id]
  (map->PreAuthorization (c/api-get account (c/path "pre_authorizations" id) {})))

(defn pre-authorizations
  "Retrieve merchant's pre-authorizations or a single pre-authorization.

  Takes as arguments either the account-map and no other arguments, or the
  account-map and a map of params.

  Examples:

    (pre-authorizations account)
    (pre-authorizations account {:id \"0K636ZDWM9\"})
    (pre-authorizations account {:per_page 5 :page 2})"
  ([account] (pre-authorizations account {}))
  ([account {:keys [id] :as params}]
     (if id
       (-> (c/api-get account (c/path "pre_authorizations" id) {})
           map->PreAuthorization)
       (let [path (c/path "merchants" (:merchant-id account) "pre_authorizations")
             preauths (c/api-get account path params)]
         (map map->PreAuthorization preauths)))))

;; ## Resource creation functions
;;
(defn new-bill
  "Returns the Connect URL for a new Bill.

  Required map keys: `:amount`."
  [account {:keys [amount] :as opts}]
  {:pre [(number? amount)
         (> amount 1.0)]}
  (let [params (assoc opts :amount (bigdec amount))]
    (c/new-limit-url account "bill" params)))

(defn create-bill
  "Creates a new bill under an existing pre-authorization."
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

;; ## Resource confirmation
;;
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
