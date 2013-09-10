(ns bson2csv.core
	(:require 
		[clojure.java.io :as io]
		[clojure.data.csv :as csv]
		[clojure.string :as s])
	(:use 
		[gloss.io]
		[bson2csv.bson]
		[clj-time.format :only [formatter unparse]]
		[clj-time.coerce :only [from-long]])
	(:import java.io.File)
	(:gen-class :main true))

(defn le32 [x]
	"Converts given 4 bytes integer to a little-endian one"
	(.getInt (.order (java.nio.ByteBuffer/wrap x) java.nio.ByteOrder/LITTLE_ENDIAN)))

(defn read-bytes [is length]
	"Reads given number of bytes from the input-stream (is) and 
		returns byte array or nil if nothing was read"
	(let [data (byte-array length)]
		(if (< (.read is data) length) nil data)))

(defn read-doc [is]
	"Reads BSON Document in three steps: 
		reading 4 bytes header first, 
		then reading body itself 
		and then skipping terminating \0 byte"
	(if-let [doc-header (read-bytes is 4)]
		(let [doc-len (- (le32 doc-header) 5)] ; doc size - 4 (header) - 1 (terminating \0)
			(let [doc (decode bson-doc-hack (read-bytes is doc-len))]
				(read-bytes is 1) ; read (skip) \0 terinating byte at the end (see bson spec)
				doc))
		nil))

(defn lazy-bson-docs [filename]
	"Lazy sequence for given BSON file"
	(letfn [(helper [is]
		(lazy-seq
			(if-let [data (read-doc is)]
				(cons data (helper is))
				(do (.close is) nil))))]
		(helper (io/input-stream filename))))

(defn filter-name [coll keys]
	"Filters collection of maps to leave only maps which :name key values are in keys"
	(filter #(some #{(:name %)} keys) coll))

(defn bson-fields [filename]
	"Returns vector of BSON file field names"
	(with-open [is (io/input-stream filename)]
		(map (fn [x] (:name x)) (read-doc is))))

(defn format-datetime [timestamp]
	(let [date (from-long timestamp)]
		(let [f (formatter "yyyy-MM-dd HH:mm")]
			(unparse f date))))

(defn prep_value [doc]
	(if (= :b-datetime (:type doc))
		(format-datetime (:value doc))
		(:value doc)))

(defn convert [filename]
	"Converts given BSON file to CSV"
	(let [csv-name (s/reverse (s/replace-first (s/reverse filename) "nosb" "vsc"))]
		(let [field-names (bson-fields filename)]
			(with-open [out-file (io/writer csv-name)]
				(csv/write-csv out-file (vector field-names))
				(csv/write-csv out-file (map #(map prep_value %) (lazy-bson-docs filename)))))))

(defn print-help []
	(println "BSON to CSV converter. USAGE: bson2csv file.bson"))

(defn -main
	[& args]
	(if (empty? args)
		(print-help)
		(convert (first args))))