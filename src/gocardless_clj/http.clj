(ns gocardless-clj.http
  (:require [clj-http.client :as client]))

(defn ua-string []
  (format "gocardless-clj/v%s" (System/getProperty "gocardless-clj.version")))

(defn construct-headers
  "Construct the headers necessary for talking to the API."
  [account]
  {"Authorization" (format "Bearer %s" (:access-token account))
   "Accept" "application/json"
   "User-Agent" (ua-string)})

(defn path
  "Join parts with `/`."
  [& parts]
  (clojure.string/join "/" parts))

(defn api-url
  "Produce a correct URL for a resource, taking the environment into account."
  [uri account]
  (if (.startsWith uri "http")
    uri
    (condp = (:environment account)
      :live (format "https://gocardless.com/api/v1/%s" uri)
      :sandbox (format "https://sandbox.gocardless.com/api/v1/%s" uri))))

(defn do-request
  "Worker function for `api-get`, `api-post` and `api-put`."
  [method url headers params]
  (-> (client/request {:method method
                       :url url
                       :headers headers
                       :as :json})
      :body))

(defn api-get
  "Do a GET request to the API."
  [uri account]
  (do-request :get (api-url uri account) (construct-headers account) {}))

(defn api-post
  "Do a POST request to the API."
  [uri account]
  (do-request :post (api-url uri account) (construct-headers account) {}))

(defn api-put
  "Do a PUT request to the API."
  [uri account]
  (do-request :put (api-url uri account) (construct-headers account) {}))
