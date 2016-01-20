(ns gocardless-clj.resources
  (:require [gocardless-clj.protocols :refer :all]
            [gocardless-clj.client :as client]))

(defrecord Bill [id]
  gocardless-clj.protocols/PCancellable
  (cancelable? [resource]
    (true? (:can_be_cancelled resource)))

  (cancel [resource account]
    (when (cancelable? resource)
      (client/api-put account (client/path (:uri resource) "cancel") {})
      true))

  gocardless-clj.protocols/PRetriable
  (retriable? [resource]
    (true? (:can_be_retried resource)))

  (retry [resource account]
    (when (retriable? resource)
      (client/api-post account (client/path (:uri resource) "retry") {})
      true)))

(defrecord Subscription [id]
  gocardless-clj.protocols/PCancellable
  (cancelable? [resource]
    (not= "cancelled" (:status resource)))

  (cancel [resource account]
    (when (cancelable? resource)
      (client/api-put account (client/path (:uri resource) "cancel") {})
      true)))

(defrecord PreAuthorization [id]
  gocardless-clj.protocols/PCancellable
  (cancelable? [resource]
    (not= "cancelled" (:status resource)))

  (cancel [resource account]
    (when (cancelable? resource)
      (client/api-put account (client/path (:uri resource) "cancel") {})
      true)))
