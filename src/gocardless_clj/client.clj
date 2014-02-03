(ns gocardless-clj.client
  (:require [clojure.walk :refer [stringify-keys]]
            [clj-http.client :as client]
            [cheshire.core :as json]
            [clj-time.core :as time]
            [gocardless-clj.signature :refer [sign-params normalise-params]]))

(defn- ua-string []
  (format "gocardless-clj/v%s" (System/getProperty "gocardless-clj.version")))

(defn- underscorize-keys
  "Recursively replace all dashes with underscores in all the keys of m.

  From clojure.walk/stringify-keys."
  [m]
  (let [f (fn [[k v]] [(clojure.string/replace k "-" "_") v])]
    (clojure.walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m)))

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

(def ^:private secure-random (java.security.SecureRandom.))
(defn- generate-nonce
  "Generate a nonce for a Connect request."
  []
  (-> (java.math.BigInteger. 256 secure-random)
      (.toString 32)))

(defn api-url
  "Produce a correct URL for retrieving resource(s)."
  [uri account]
  (if (.startsWith uri "http")
    uri
    (condp = (:environment account)
      :live (format "https://gocardless.com/api/v1/%s" uri)
      :sandbox (format "https://sandbox.gocardless.com/api/v1/%s" uri))))

(defn connect-url
  "Produce a correct connect base URL."
  [resource-type account]
  (condp = (:environment account)
    :live (format "https://gocardless.com/connect/%ss/new" resource-type)
    :sandbox (format "https://sandbox.gocardless.com/connect/%ss/new" resource-type)))

(defn new-limit-url
  "Produce a correct connect URL for creating a limit."
  [resource-type limit-params account]
  (let [url (connect-url resource-type account)
        limit-params (-> limit-params stringify-keys underscorize-keys)
        limit-params (assoc limit-params "merchant_id" (:merchant-id account))

        meta-params (select-keys limit-params ["redirect_uri" "cancel_uri" "state"])
        limit-params (dissoc limit-params "redirect_uri" "cancel_uri" "state")

        base-params {"nonce" (generate-nonce)
                     "timestamp" (str (time/now))
                     "client_id" (:app-id account)
                     resource-type limit-params}
        params (merge base-params meta-params)
        signature (-> params (sign-params (:app-secret account)))
        params (merge {"signature" signature} params)]
    (str url
         "?"
         (-> params normalise-params))))

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
