(ns gocardless-clj.t-signature
  (:use midje.sweet)
  (:use gocardless-clj.signature)
  (:require [gocardless-clj.protocols :refer [flatten-params]]))

(facts "generating namespaced keys"
  (new-ns "some-key") => "some-key[]"
  (new-ns "some-key" "new-key") => "some-key[new-key]")

(facts "flattening parameters"
  (flatten-params (bigdec 10) "ns") => [["ns" "10"]]
  (flatten-params "string" "ns") => [["ns" "string"]]

  (let [cars {"cars" ["BMW" "Fiat" "VW"]}
        nested {"user" {"name" "Fred" "age" "10"}}
        nested-cars {"user" {"name" "Fred" "cars" ["BMW" "Fiat"]}}]
    (flatten-params cars nil) => [["cars[]" "BMW"] ["cars[]" "Fiat"] ["cars[]" "VW"]]
    (flatten-params nested nil) => [["user[name]" "Fred"] ["user[age]" "10"]]
    (flatten-params nested-cars nil) => [["user[name]" "Fred"]
                                              ["user[cars][]" "BMW"]
                                              ["user[cars][]" "Fiat"]]))

(facts "normalising key-value pairs"
  (normalise-keyval ["key" "val"]) => "key=val"
  (normalise-keyval ["key[]" "val"]) => "key%5B%5D=val"
  (normalise-keyval ["key[subkey]" "val"]) => "key%5Bsubkey%5D=val"
  (normalise-keyval ["key[subkey][]" "val"]) => "key%5Bsubkey%5D%5B%5D=val")

(facts "normalising params"
  (let [p {"user" {"email" "me@example.com" "age" (bigdec 30)}}]
    (normalise-params p) => "user%5Bage%5D=30&user%5Bemail%5D=me%40example.com"))

(facts "signing params"
  (let [app-secret "5PUZmVMmukNwiHc7V/TJvFHRQZWZumIpCnfZKrVYGpuAdkCcEfv3LIDSrsJ+xOVH"
        params {"user" {"email" "fred@example.com" "age" (bigdec 30)}}]
    ;; example from https://developer.gocardless.com/#signing-the-parameters
    (sign-params params app-secret)
    => "763f02cb9f998a5e06fda2b790bedd503ba1a34fd7cbf9e22f8ce562f73f0470"))
