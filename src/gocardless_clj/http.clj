(ns gocardless-clj.http
  (:require [clj-http.client :as client]))

(defn ua-string []
  (format "gocardless-clj/v%s" (System/getProperty "gocardless-clj.version")))

(defn construct-headers
  [account]
  {"Authorization" (format "Bearer %s" (:access-token account))
   "Accept" "application/json"
   "User-Agent" (ua-string)})

(defn path
  "Join parts with `/`."
  [& parts]
  (clojure.string/join "/" parts))

(defn api-url
  [uri account]
  (if (.startsWith uri "http")
    uri
    (if (= (:environment account) :live)
      (format "https://gocardless.com/api/v1/%s" uri)
      (format "https://sandbox.gocardless.com/api/v1/%s" uri))))

(defn do-request
  [method url headers params]
  (-> (client/request {:method method
                       :url url
                       :headers headers
                       :as :json})
      :body))

(defn api-get
  [uri account]
  (do-request :get (api-url uri account) (construct-headers account) {}))

(defn api-post
  [uri account]
  (do-request :post (api-url uri account) (construct-headers account) {}))

(defn api-put
  [uri account]
  (do-request :put (api-url uri account) (construct-headers account) {}))
