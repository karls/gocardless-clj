;; ## Signature calculation functions
;;
;; Implements signature calculation according to the
;; [signature guide](https://developer.gocardless.com/#signature-guide).
;;
(ns gocardless-clj.signature
  (:require [pandect.core :refer [sha256-hmac]]
            [gocardless-clj.protocols :refer [flatten-params]])
  (:import java.net.URLEncoder))

(defn new-ns
  ([ns] (str ns "[]"))
  ([ns k]
     (if ns
       (str ns "[" k "]")
       k)))

(extend-protocol gocardless-clj.protocols/PFlattenable
  clojure.lang.PersistentArrayMap
  (flatten-params [coll ns]
    (let [pairs (map #(flatten-params %2 (new-ns ns %1)) (keys coll) (vals coll))]
      (if (empty? pairs)
        []
        (apply concat pairs))))

  clojure.lang.PersistentHashMap
  (flatten-params [coll ns]
    (let [pairs (map #(flatten-params %2 (new-ns ns %1)) (keys coll) (vals coll))]
      (if (empty? pairs)
        []
        (apply concat pairs))))

  clojure.lang.PersistentVector
  (flatten-params [coll ns]
    (let [pairs (map #(flatten-params %1 (new-ns ns)) coll)]
      (if (empty? pairs)
        []
        (apply concat pairs))))

  java.lang.String
  (flatten-params [string ns]
    [[ns string]])

  clojure.lang.Keyword
  (flatten-params [kw ns]
    [[ns (name kw)]])

  java.lang.Long
  (flatten-params [number ns]
    [[ns (str number)]])

  java.math.BigDecimal
  (flatten-params [number ns]
    [[ns (str number)]]))

(defn percent-encode
  "Percent-encode a string."
  [s]
  (URLEncoder/encode (name s) "UTF-8"))

(defn normalise-keyval
  "Normalises a key-value pair.

  Percent-encodes both key and value and joins them with `=`."
  [keyval-pair]
  (clojure.string/join "=" (map percent-encode keyval-pair)))

(defn normalise-params
  "Genrates a percent-encoded query string.

  Individual keys and values are joined with `=`, joined key-value pairs
  are joined with `&`."
  [params]
  (let [flattened-sorted (-> params (flatten-params nil) sort)]
    (clojure.string/join "&" (map normalise-keyval flattened-sorted))))

(defn sign-params
  "Sign the normalised params with SHA256 using a key. The key in this case is
  the app secret."
  [params key]
  (-> params normalise-params (sha256-hmac key)))
