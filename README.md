# bson2csv

A Clojure utility to convert BSON (MongoDB dumps) files to CSV (Comma Separated Values).

The following value types are currently supported: int32, int64, double, string, bool, datetime, object-id, null.

## Usage

lein run -- input.bson

or generate a single jar file with "lein uberjar" and then run it:
 
java -jar bson2csv.jar input.bson

## License

Copyright © 2013 Alexey Pegov

Distributed under the Eclipse Public License, the same as Clojure.
