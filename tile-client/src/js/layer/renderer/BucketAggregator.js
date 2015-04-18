/*
 * Copyright (c) 2013 Oculus Info Inc.
 * http://www.oculusinfo.com/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

( function() {

    "use strict";

    /*
    {
        "score": {
            "total": 6,
            "texts": [
                {
                    "score": 4,
                    "text": "I'd hammer in the morning"
                },
                {
                    "score": 4,
                    "text": "Never odd or even"
                },
                {
                    "score": 4,
                    "text": "(But the fought like the devil when his temper got stirred)"
                }
            ]
        },
        "topic": "a"
    }
    */

    /*
    {
        "score": {
            "total": "$sum",
            "texts": "$append"
        },
        "topic": "$id"
    }
    {
        "$id": "topic",
        "$sort": "score.total",
        "$append": ["score.texts"],
        "$sum":  ["score.total"]
    }
    {
        group: {
            topic: "$id"
        },
        aggregate: {
            score: {
                total: "$sum",
                texts: "$append"
            },
        },
        sort: {
        score: {
            total: "$asc"
        }
    }
    */

    // build the operation paths, depth first
    function buildPathsRecursive( paths, path, obj ) {
        var key;
        if ( typeof obj === "string" ) {
            // base case
            paths.push( path.concat( obj ) );
        } else {
            // object
            for ( key in obj ) {
                if ( obj.hasOwnProperty( key ) ) {
                    buildPathsRecursive( paths, path.concat( key ), obj[ key ] );
                }
            }
        }
    }

    // traverse all paths to '$' vars, and store them in an array
    function buildPaths( spec ) {
        var paths = [];
        buildPathsRecursive( paths, [], spec );
        return paths;
    }

    function getIdPath( paths ) {
        var path,
            i;
        for ( i=0; i<paths.length; i++ ) {
            path = paths[i];
            if ( path[ path.length-1 ] === "$id" ) {
                paths.splice( i, 1 );
                return path;
            }
        }
    }

    function sum( a, b ) {
        var key, i;
        if ( a instanceof Array ) {
            for ( i=0; i<a.length; i++ ) {
                a[i] = sum( a[i], b[i] );
            }
        } else if ( a instanceof Object ) {
            for ( key in a ) {
                if ( a.hasOwnProperty( key ) ) {
                    a[key] = sum( a[key], b[key] );
                }
            }
        } else {
            a += b;
        }
        return a;
    }

    function append( a, b ) {
        var key;
        if ( a instanceof Array ) {
            a = a.concat( b );
        } else {
            for ( key in a ) {
                if ( a.hasOwnProperty( key ) ) {
                    a[key] = append( a[key], b[key] );
                }
            }
        }
        return a;
    }

    function max( a, b ) {
        var key, i;
        if ( a instanceof Array ) {
            for ( i=0; i<a.length; i++ ) {
                a[i] = max( a[i], b[i] );
            }
        } else if ( a instanceof Object ) {
            for ( key in a ) {
                if ( a.hasOwnProperty( key ) ) {
                    a[key] = max( a[key], b[key] );
                }
            }
        } else {
            a = Math.max( a, b );
        }
        return a;
    }

    function min( a, b ) {
        var key, i;
        if ( a instanceof Array ) {
            for ( i=0; i<a.length; i++ ) {
                a[i] = min( a[i], b[i] );
            }
        } else if ( a instanceof Object ) {
            for ( key in a ) {
                if ( a.hasOwnProperty( key ) ) {
                    a[key] = min( a[key], b[key] );
                }
            }
        } else {
            a = Math.min( a, b );
        }
        return a;
    }

    // iterate over spec, and then for
    function applyToBuckets( paths, buckets ) {
        var agg = JSON.parse( JSON.stringify( buckets[0] ) ), // copy
            operator,
            aggItr,
            bucketItr,
            path,
            bucket,
            key,
            i,
            j;
        // for each bucket of data
        for ( i=1; i<buckets.length; i++ ) {
            bucket = buckets[i];
            // for each path
            for ( j=0; j<paths.length; j++ ) {
                bucketItr = bucket;
                aggItr = agg;
                path = paths[j].slice( 0 );
                while ( path.length > 2 ) {
                    key = path.shift();
                    aggItr = aggItr[ key ];
                    bucketItr = bucketItr[ key ];
                }
                // get key for value
                key = path.shift();
                // get the poerator value
                operator = path.shift();
                switch ( operator ) {
                    case "$sum":
                        aggItr[ key ] = sum( aggItr[ key ], bucketItr[ key ] );
                        break;
                    case "$append":
                        aggItr[ key ] = append( aggItr[ key ], bucketItr[ key ] );
                        break;
                    case "$max":
                        aggItr[ key ] = max( aggItr[ key ], bucketItr[ key ] );
                        break;
                    case "min":
                        aggItr[ key ] = min( aggItr[ key ], bucketItr[ key ] );
                        break;
                }
            }
        }
        return agg;
    }

    /**
     * Instantiate a BucketOperation object.
     * @class BucketOperation
     * @classdesc
     */
    function BucketAggregator( spec ) {
        this.paths = buildPaths( spec );
        this.idPath = getIdPath( this.paths );
    }

    BucketAggregator.prototype.aggregate = function( data ) {
        var buckets = data.tile.meta.map.bins,
            paths = this.paths,
            idPath = this.idPath,
            bucketsById = {},
            aggBuckets = [],
            bucket,
            value,
            key,
            i,
            j,
            k;
        for ( i=0; i<buckets.length; i++ ) {
            bucket = buckets[i];
            if ( bucket ) {
                for ( j=0; j<bucket.length; j++ ) {
                    value = bucket[j];
                    for ( k=0; k<idPath.length-1; k++ ) {
                        key = idPath[k];
                        value = value[ key ];
                    }
                    bucketsById[ value ] = bucketsById[ value ] || [];
                    bucketsById[ value ].push( bucket[j] );
                }
            }
        }
        for ( key in bucketsById ) {
            if ( bucketsById.hasOwnProperty( key ) ) {
                aggBuckets.push( applyToBuckets( paths, bucketsById[ key ] ) );
            }
        }
        data.tile = aggBuckets;
        return data;
    };

    module.exports = BucketAggregator;

}());
