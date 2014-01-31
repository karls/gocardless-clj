(ns gocardless-clj.signature
  (:require [pandect.core :refer [sha256-hmac]])
  (:import java.net.URLEncoder))

(defn new-ns
  ([ns] (str ns "[]"))
  ([ns k]
     (if ns
       (str ns "[" k "]")
       k)))

(defprotocol PFlattable
  (flatten-params [coll ns]
    "Flatten params according to
    https://developer.gocardless.com/#constructing-the-parameter-array"))

(extend-protocol PFlattable
  clojure.lang.PersistentHashMap
  (flatten-params [coll ns]
    (let [pairs (map #(flatten-params %2 (new-ns ns %1)) (keys coll) (vals coll))]
      (if (empty pairs)
        []
        (apply concat pairs))))

  clojure.lang.PersistentArrayMap
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

  java.lang.Long
  (flatten-params [number ns]
    [[ns (str number)]]))

(defn percent-encode
  "Percent-encode a string."
  [s]
  (URLEncoder/encode s "UTF-8"))

(defn normalise-keyval
  "Normalises a key-value pair.

  Percent-encodes both key and value and joins them with `=`."
  [keyval-pair]
  (apply str (interpose "=" (map percent-encode keyval-pair))))

(defn normalise-params
  "Genrates a percent-encoded query string.

  Individual keys and values are joined with `=`, joined key-value pairs
  are joined with `&`."
  [params]
  (let [flattened-sorted (-> params (flatten-params nil) sort)]
    (apply str (interpose "&" (map normalise-keyval flattened-sorted)))))

(defn sign-params
  [params key]
  (-> params normalise-params (sha256-hmac key)))
