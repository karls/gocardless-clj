# gocardless-clj

A Clojure library for the GoCardless API.

## Status

The library currently implements most of the functionality for merchants, as
described in the GoCardless API docs. That means that parter integrations are
not supported.

Although the API is pretty clean and should be fairly easy to use, some nice
things are currently not in place or are rough around the edges.

* Automatic pagination of results is not supported, which means that you'll have
  to paginate manually.
* Filtering **should** work out of the box, as passing query string params
  is supported.
* Handling webhooks are currently not supported.
* Pre-populating fields for Connect flow pages is supported and should work out
  of the box.

See the GoCardless API docs for notes on filtering and pagination.

## Usage

All the core functionality is available in `core.clj`.

```clj
(ns user
  (:require [gocardless-clj.core :refer :all]))

(def account-details {:environment :sandbox (or :live)
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
(merchant bills "bill id")

;; cancel a bill
(def bill (merchant bills "bill id"))
(merchant cancel bill)

;; get the first page of your subscriptions, 10 per page
(merchant subscriptions {:per_page 10})

;; create a new bill with minimal parameters
(merchant new-bill {:amount 10.0})

;; create a new bill, but pass in more information
(merchant new-bill {:amount 10.0
	                :name "My first bill"
					:user {:email "customer.email@example.com
						   :first\_name "Joe"
						   :last\_name "Bloggs"}})

;; When a customer has gone through they will be redirected to
;; a URL specified by Redirect URI in your Dashboard (can be
;; over-ridden in the params hash when creating a new bill,
;; subscription or pre-authorization).
;; Once the customer is redirected back to your site, you'll need
;; to confirm the resource, but calling (confirm-resource) and
;; passing in the parameters in the request, along with the account
;; details.
(def params (:query-params request))
(confirm-resource params account)
```

## Contributing

Contributions are welcome!

If you're interested in using this for a parter integration or need
more/better functionality, feel free to get in touch via Github issues,
or submit a patch.

## License

Copyright © 2014 Karl Sutt

Distributed under MIT license, see LICENSE.
