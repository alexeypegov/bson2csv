(ns ^{:doc "Gloss frames for encode/decode BSON data"
	  :author "Alexey Pegov"}
	bson2csv.bson
	(:use [gloss core io]))

; BSON string is a c-style string ended with a \0
(def cstring (string :utf-8 :delimiters [0]))

(defn bytes2hex [bytes]
	"Converts array of bytes to HEX string"
	(apply str (map #(format "%02x" %) bytes)))

(defn hex2bytes [hex]
	"Converts HEX string to array of bytes"
	(byte-array (map (fn [[x y]] (unchecked-byte (Integer/parseInt (str x y) 16))) (partition 2 hex))))

(defn decode-oid [s]
	"Decodes BSON Object-Id from the given sequence of ByteBuffers"
	(let [buffer (contiguous s)]
		(let [bytes (byte-array (.remaining buffer))]
			(.get buffer bytes 0 (alength bytes))
			(bytes2hex bytes))))

(defn encode-oid [hex]
	(to-buf-seq (hex2bytes hex)))

; MongoDB object-id, see http://docs.mongodb.org/manual/reference/object-id/
(def object-id (compile-frame (finite-block 12) encode-oid decode-oid))

; BSON string prefixed with int32-le length and terminated with \0
(defcodec bson-string (finite-frame :int32-le (string :utf-8 :delimiters [0])))

; types of elements
(defcodec b-type (enum :byte {
	:b-double 	0x01, 
	:b-str 		0x02, 
	:b-oid 		0x07, 
	:b-bool 	0x08, 
	:b-datetime	0x09,
	:b-null		0x0A, 
	:b-int32 	0x10, 
	:b-int64 	0x12}))

; individual element codecs
(defcodec b-double (ordered-map :type :b-double :name cstring :value :float64-le))
(defcodec b-str (ordered-map :type :b-str :name cstring :value bson-string))
(defcodec b-oid (ordered-map :type :b-oid :name cstring :value object-id))
(defcodec b-bool (ordered-map :type :b-bool :name cstring :value :byte))
(defcodec b-datetime (ordered-map :type :b-datetime :name cstring :value :int64-le))
(defcodec b-null (ordered-map :type :b-null :name cstring))
(defcodec b-int32 (ordered-map :type :b-int32 :name cstring :value :int32-le))
(defcodec b-int64 (ordered-map :type :b-int64 :name cstring :value :int64-le))

; codec for typed element
(defcodec elements
	(header 
		b-type
		{
			:b-double	b-double,
			:b-str      b-str,
			:b-oid 		b-oid,
			:b-bool		b-bool,
			:b-datetime	b-datetime,
			:b-null		b-null,
			:b-int32 	b-int32, 
			:b-int64 	b-int64
		}
		:type))


; BSON Document codec prefixed with it's length (including prefix length itself) and suffixed with a \0
; TODO: Suited for writing and NOT SUITED FOR READING since \0 inside strings are treated as bson-doc delimeters :(
(defcodec bson-doc
	(finite-frame (prefix :int32-le #(- % 4) #(+ % 4)) (repeated elements :prefix :none :delimiters [0])))

; BSON Document codec w/o delimiters & prefix (for manual decoding)
(defcodec bson-doc-hack 
	(repeated elements :prefix :none))

; BSON file codec: a list of BSON Documents
(defcodec bson
	(repeated bson-doc :prefix :none))