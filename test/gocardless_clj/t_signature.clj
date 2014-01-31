(ns gocardless-clj.t-signature
  (:use midje.sweet)
  (:require [gocardless-clj.signature :as sign]))

(facts "about generating keys"
  (sign/new-ns "some-key") => "some-key[]"
  (sign/new-ns "some-key" "new-key") => "some-key[new-key]")

(facts "about flattening parameters"
  (sign/flatten-params 10 "ns") => [["ns" "10"]]
  (sign/flatten-params "string" "ns") => [["ns" "string"]]

  (let [cars {"cars" ["BMW" "Fiat" "VW"]}
        nested {"user" {"name" "Fred" "age" "10"}}
        nested-cars {"user" {"name" "Fred" "cars" ["BMW" "Fiat"]}}]
    (sign/flatten-params cars nil) => [["cars[]" "BMW"] ["cars[]" "Fiat"] ["cars[]" "VW"]]
    (sign/flatten-params nested nil) => [["user[name]" "Fred"] ["user[age]" "10"]]
    (sign/flatten-params nested-cars nil) => [["user[name]" "Fred"]
                                              ["user[cars][]" "BMW"]
                                              ["user[cars][]" "Fiat"]]))

(facts "about normalising key-value pairs"
  (sign/normalise-keyval ["key" "val"]) => "key=val"
  (sign/normalise-keyval ["key[]" "val"]) => "key%5B%5D=val"
  (sign/normalise-keyval ["key[subkey]" "val"]) => "key%5Bsubkey%5D=val"
  (sign/normalise-keyval ["key[subkey][]" "val"]) => "key%5Bsubkey%5D%5B%5D=val")

(facts "about normalising params"
  (let [p {"user" {"email" "me@example.com" "age" 30}}]
    (sign/normalise-params p) => "user%5Bage%5D=30&user%5Bemail%5D=me%40example.com"))

(facts "about signing params"
  (let [app-secret "5PUZmVMmukNwiHc7V/TJvFHRQZWZumIpCnfZKrVYGpuAdkCcEfv3LIDSrsJ+xOVH"
        params {"user" {"email" "fred@example.com" "age" 30}}]
    ;; example from https://developer.gocardless.com/#signing-the-parameters
    (sign/sign-params params app-secret) => "763f02cb9f998a5e06fda2b790bedd503ba1a34fd7cbf9e22f8ce562f73f0470"))
