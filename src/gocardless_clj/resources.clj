(ns gocardless-clj.resources
  (:require [gocardless-clj.protocols :refer :all]
            [gocardless-clj.http :as http]))

(defrecord Bill [id]
  gocardless-clj.protocols/PCancellable
  (cancelable? [resource]
    (true? (:can_be_cancelled resource)))

  (cancel [resource account]
    (when (cancelable? resource)
      (-> (http/api-put account (http/path (:uri resource) "cancel"))
          map->Bill)))

  gocardless-clj.protocols/PRetriable
  (retriable? [resource]
    (true? (:can_be_retried resource)))

  (retry [resource account]
    (when (retriable? resource)
      (-> (http/api-post account (http/path (:uri resource) "retry"))
          map->Bill))))

(defrecord Subscription [id]
    gocardless-clj.protocols/PCancellable
  (cancelable? [resource]
    (not (= "cancelled" (:status resource))))

  (cancel [resource account]
    (when (cancelable? resource)
      (-> (http/api-put account (http/path (:uri resource) "cancel"))
          map->Subscription))))

(defrecord PreAuthorization [id]
  gocardless-clj.protocols/PCancellable
  (cancelable? [resource]
    (not (= "cancelled" (:status resource))))

  (cancel [resource account]
    (when (cancelable? resource)
      (-> (http/api-put account (http/path (:uri resource) "cancel"))
          map->PreAuthorization))))
