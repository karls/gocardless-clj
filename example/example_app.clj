(ns example-app
  (:use compojure.core)
  (:use gocardless-clj.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :refer [redirect]]))

(def account {:environment :sandbox
              :merchant-id "merchant id"
              :app-id "app identifier"
              :app-secret "app secret"
              :access-token "access token"})

(defroutes app-routes
  (GET "/"
       []
       "Hello. Go to /example-bill, /example-subscription or /example-preauth")

  (GET "/example-bill"
       []
       (redirect (new-bill account {:amount 10.0 :name "Example bill"})))

  (GET "/example-subscription"
       []
       (redirect (new-subscription account {:amount 10.0
                                            :interval_length 1
                                            :interval_unit "month"})))

  (GET "/example-preauth"
       []
       (redirect (new-pre-authorization account {:max_amount 100.0
                                                 :interval_length 3
                                                 :interval_unit "month"})))
  (GET "/confirm"
       request
       (str (confirm-resource account (:query-params request))))

  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
