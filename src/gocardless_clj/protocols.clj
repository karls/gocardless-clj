(ns gocardless-clj.protocols)

(defprotocol PFlattenable
  (flatten-params [coll ns]
    "Flatten params according to
    https://developer.gocardless.com/#constructing-the-parameter-array."))

(defprotocol PCancellable
  (cancelable? [resource] "Check if the resource can be cancelled.")
  (cancel [resource account] "Cancel a resource."))

(defprotocol PRetriable
  (retriable? [resource] "Check if the resource can be retried.")
  (retry [resource account] "Retry a resource."))
