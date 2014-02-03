# gocardless-clj

A Clojure library for the GoCardless API.

## Status

[![Build Status](https://travis-ci.org/karls/gocardless-clj.png?branch=master)](https://travis-ci.org/karls/gocardless-clj)

The library implements most of the functionality for merchants, as
described in the GoCardless API docs. Partner integrations are currently not
supported.

Although the API is pretty clean and should be fairly easy to use, some nice
things are not yet in place or are rough around the edges. This also means
that the API is in a bit of a flux and may change at any time.

* Automatic pagination of results is not supported, which means that you'll have
  to paginate manually.
* Filtering **should** work out of the box, as passing query string params
  is supported.
* Handling webhooks are currently not supported.
* Pre-populating fields for Connect flow pages is supported and should work out
  of the box.

See the GoCardless API docs for notes on filtering and pagination.

### Installing

`gocardless-clj` is available on Clojars, at https://clojars.org/gocardless-clj.
Just copy and paste the relevant line from Clojars into your *project.clj*.

## Usage

All the core functionality is available in *core.clj*.

```clj
(ns user
  (:require [gocardless-clj.core :refer :all]))

;; :environment may be either :sandbox or :live
(def account-details {:environment :sandbox
                      :merchant-id "your merchant id"
                      :app-id "your app id"
                      :app-secret "your app secret"
                      :access-token "your access token"})

;; make-account returns a function
(def merchant (make-account account-details))

;; get your account details
(merchant details)

;; get the first page of your bills
(merchant bills)

;; get a specific bill
(merchant bills "id")

;; cancel a bill
(let [my-bill (merchant bills "id")]
  (cancelable? my-bill) ;; => true
  (merchant cancel my-bill))

;; get the first page of your subscriptions, 10 per page
(merchant subscriptions {:per_page 10})

;; create a new bill with minimal parameters
(merchant new-bill :amount 10.0)

;; create a new bill, but pass in more information
(merchant new-bill {:amount 10.0
	                :name "My first bill"
					:user {:email "customer.email@example.com"
						   :first_name "Joe"
						   :last_name "Bloggs"}})

;; When a customer has gone through they will be redirected to
;; a URL specified by Redirect URI in your Dashboard (can be
;; over-ridden in the params hash when creating a new bill,
;; subscription or pre-authorization).
;; Once the customer is redirected back to your site, you'll need
;; to confirm the resource, but calling (confirm-resource) and
;; passing in the parameters in the request, along with the account
;; details.
(let[params (:query-params request)]
  (confirm-resource params account))
```

### Internal API notes

In order to provide a nice and consitent API externally, some compromises
had to be made internally in terms of clarity of the code.

The first function that's worth investigating it `(make-account)` located in
*core.clj*. Essentially, it takes a map of account details and produces a
multi-arity function that closes over the account details. This makes it possible
to declare your merchant account with `(make-account account-details)` and
use the returned closure for making requests.

The returned closure takes either a function, a function and some param (an ID
or a params map), or a function and some keys and values. Passing a function
and a map is equivalent to passing a function and keys-values.

The functions for retrieving resources (`customers`, `bills` etc) are
implemented as multimethods that have different implementations based on the
class of the first argument as well as the number of arguments they receive.

Each core function takes the map of account details as its last argument, and
it is the job of the closure, returned from `(make-account)`, to insert it as
the last argument of the supplied function.

For example:

```clj
;; account-details is a map of account details
(def merchant (make-account account-details))

;; the merchant closure inserts the map of account details as the last argument
;; of bills
(merchant bills)

;; it is equivalent to
(bills account-details)

;; (merchant bills "ID") is equivalent to (bills "ID" account-details) and so on
```

## Contributing

Contributions are welcome!

If you're interested in using this for a parter integration or need
more/better functionality, feel free to get in touch via Github issues,
or submit a patch.

## License

Copyright © 2014 Karl Sutt

Distributed under MIT license, see LICENSE.
