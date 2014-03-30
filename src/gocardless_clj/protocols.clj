(ns gocardless-clj.protocols)

(defprotocol PFlattenable
  "Flatten params according to the
  [signature guide](https://developer.gocardless.com/#signature-guide).

  `gocardless-clj.signature` namespace contains the extension of this
  protocol for different data types/structures."
  (flatten-params [coll ns]))

(defprotocol PCancellable
  (cancelable? [resource] "Check if the resource can be cancelled.")
  (cancel [resource account] "Cancel a resource."))

(defprotocol PRetriable
  (retriable? [resource] "Check if the resource can be retried.")
  (retry [resource account] "Retry a resource."))
