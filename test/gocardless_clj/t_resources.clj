(ns gocardless-clj.t-resources
  (:use midje.sweet)
  (:use gocardless-clj.resources)
  (:use gocardless-clj.protocols))

(facts "about cancelling and retrying bills"
  (fact "a bill can't be cancelled if :can_be_cancelled = false"
    (cancelable? (map->Bill {:can_be_cancelled false})) => false)
  (fact "a bill can be cancelled if :can_be_cancelled = true"
    (cancelable? (map->Bill {:can_be_cancelled true})) => true)
  (fact "a bill can't be retried if :can_be_retried = false"
    (retriable? (map->Bill {:can_be_retried false})) => false)
  (fact "a bill can be retried if :can_be_retried = true"
    (retriable? (map->Bill {:can_be_retried true})) => true))

(facts "about cancelling subscription"
  (fact "a subscription can't be cancelled if its status is cancelled"
    (cancelable? (map->Subscription {:status "cancelled"})) => false)
  (fact "a subscription can be cancelled if its status is not cancelled"
    (cancelable? (map->Subscription {:status "active"})) => true))

(facts "about cancelling preauths"
  (fact "a preauth can't be cancelled if its status is cancelled"
    (cancelable? (map->PreAuthorization {:status "cancelled"})) => false)
  (fact "a preauth can be cancelled if its status is not cancelled"
    (cancelable? (map->PreAuthorization {:status "active"})) => true))
