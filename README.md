# gocardless-clj

A Clojure library for talking to the [GoCardless](https://gocardless.com) API.

GoCardless is an online payments provider, leveraging the direct debit network
in the UK (BACS) and European Union (SEPA) for low-cost bank-to-bank payments.

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
(let [params (:query-params request)]
  (confirm-resource params account))
```

### Example app

There is an example application located in `example/example_app.clj` which is a
simple [Compojure](https://github.com/weavejester/compojure/) web-app.

You should clone this repository

```sh
$ git clone https://github.com/karls/gocardless-clj
```

and change in your details in the `account-details` map, save the file
and run `lein ring server-headless` on the command-line. That should boot
a Jetty server and if you go to http://localhost:3000 and you'll see further
instructions.

That is all good, but unfortunately there is no way to confirm a resource once
you've gone through the GoCardless' Connect flow, as GoCardless cannot redirect
the customer to http://localhost:3000/confirm. Fortunately, there is a way to
fix it by using a handy tool such as
[localtunnel](https://github.com/defunctzombie/localtunnel) or
[ngrok](https://ngrok.com/).

I prefer ngrok, so, in a different terminal window, run `ngrok 3000`. It should
give back a couple of URLs:

![ngrok output](http://i.imgur.com/liuqXmg.png)

Since the URL for confirming a resource is set to `http://localhost:3000/confirm`,
as per the example application, you'll need to set the Redirect URL to
something that looks like http://6b2806c0.ngrok.com/confirm.

Copy the forwarded URL (the one that looks like http://6b2806c0.ngrok.com)
as the Redirect URL in your GoCardless Developer settings and append
`/confirm`. The customer will be effectively redirected to
http://6b2806c0.ngrok.com/confirm, which matches the confirm URL in the example
app.

Now, when you navigate to, say, http://6b2806c0.ngrok.com/example-bill
and go through the GoCardless' Connect flow, the resource should be confirmed
and the response should be something like `{:success true}`.

## Internal API notes

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

Bugs may be reported via Github issues or fork the repo, write some tests, fix
the bug and submit a pull request.

## License

Copyright © 2014 Karl Sutt

Distributed under MIT license, see LICENSE.
