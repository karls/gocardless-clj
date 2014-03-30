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
* Filtering **should** work out of the box, as passing query string parameters
  is supported.
* Handling webhooks are currently not supported.
* Pre-populating fields for Connect flow pages is supported and should work out
  of the box.

See the GoCardless API docs for notes on filtering and pagination.

### Installing

`gocardless-clj` is available on Clojars, at https://clojars.org/gocardless-clj.
Just copy and paste the relevant line from Clojars into your *project.clj*.

## Usage

All the core functionality is available in *core.clj*. Marginalia-generated API
docs are available on Github Pages. You should look at the
[section for the core namespace](http://karls.github.io/gocardless-clj/#gocardless-clj.core)

```clj
(ns user
  (:require [gocardless-clj.core :refer :all]))

;; :environment may be either :sandbox or :live
(def account {:environment :sandbox
              :merchant-id "your merchant id"
              :app-id "your app id"
              :app-secret "your app secret"
              :access-token "your access token"})

;; get your account details
(details account)

;; get the first page of your bills
(bills account)

;; get a specific bill
(bills account {:id "id"})

;; cancel a bill
(let [my-bill (bills account {:id "id"})]
  (when (cancelable? my-bill)
    (cancel my-bill account)))

;; get the first page of your subscriptions, 10 per page
(subscriptions account {:per_page 10})

;; create a new bill with minimal parameters
(new-bill account {:amount 10.0})

;; create a new bill, but pass in more information
(new-bill account {:amount 10.0
	               :name "My first bill"
				   :user {:email "customer.email@example.com"
				          :first_name "Joe"
						  :last_name "Bloggs"}})

;; When a customer has gone through they will be redirected to
;; a URL specified by Redirect URI in your Dashboard (can be
;; over-ridden in the params hash when creating a new bill,
;; subscription or pre-authorization).
;; Once the customer is redirected back to your site, you'll need
;; to confirm the resource by calling (confirm-resource) and
;; passing in the parameters in the request.
(let [params (:query-params request)]
  (confirm-resource account params))
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
a Jetty server and if you go to `http://localhost:3000` and you'll see further
instructions.

That is all good, but unfortunately there is no way to confirm a resource once
you've gone through the GoCardless' Connect flow, as GoCardless cannot redirect
the customer to `http://localhost:3000/confirm`. Fortunately, there is a way to
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

## Contributing

Contributions are welcome!

If you're interested in using this for a partner integration or need
more/better functionality, feel free to get in touch via Github issues,
or submit a patch.

If you find any bugs, open an issue via Github issues and describe the situation.
Explain what are you trying to do, what was the expected behaviour and what was
the observed behaviour. Alternatively, fork the repo, write some tests, fix the
bug and submit a pull request.

## License

Copyright © 2014 Karl Sutt

Distributed under MIT license, see LICENSE.
