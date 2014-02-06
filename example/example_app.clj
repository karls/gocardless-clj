(ns example-app
  (:use compojure.core)
  (:use gocardless-clj.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :refer [redirect]]))

(def account-details {:environment :sandbox
                      :merchant-id "merchant id"
                      :app-id "app identifier"
                      :app-secret "app secret"
                      :access-token "access token"})

(def merchant (make-account account-details))

(defroutes app-routes
  (GET "/"
       []
       "Hello. Go to /example-bill, /example-subscription or /example-preauth")

  (GET "/example-bill"
       []
       (redirect (merchant new-bill {:amount 10.0 :name "Example bill"})))

  (GET "/example-subscription"
       []
       (redirect (merchant new-subscription {:amount 10.0
                                             :interval_length 1
                                             :interval_unit "month"})))

  (GET "/example-preauth"
       []
       (redirect (merchant new-pre-authorization {:max_amount 100.0
                                                  :interval_length 3
                                                  :interval_unit "month"})))
  (GET "/confirm"
       request
       (str (merchant confirm-resource (:query-params request))))

  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
