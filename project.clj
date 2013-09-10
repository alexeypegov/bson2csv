(defproject bson2csv "0.1.0-SNAPSHOT"
  :description "BSON (Binary JSON) to CSV (Comma Separated Values) converter"
  :url "http://github.com/alexeypegov/bson2csv"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [
  				[org.clojure/clojure "1.5.1"]
  				[org.clojure/data.csv "0.1.2"]
  				[clj-time "0.6.0"]
  				[gloss "0.2.2-beta4"]]
  :main bson2csv.core)
