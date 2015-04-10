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

    var Util = require('../../util/Util' ),
        RendererUtil = require('./RendererUtil');

        // THIS NEEDS TO BE DONE ON A PER RENDERER BASIS?
        // SPECIFY GENERIC PATHS TO BIN
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
    function sumBuckets( bins, spec ) {
        var agg = {},
            bin,
            i;
        for ( i=0; i<bins.length; i++ ) {
            bin = bins[i];
            key = RendererUtil.getAttributeValue( bin, spec.idKey );
            value = RendererUtil.getAttributeValue( bin, spec.dataKey );
            agg[ key ] = agg[ key ] || {
                total: 0,
                texts: []
            };
            agg[ key ].total += value.total;
            agg[ key ].texts = agg[ key ].texts.concat( value.texts );
        }
        return agg;
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
    */

    // build the operation paths, depth first
    function buildPathsRecursive( paths, path, obj ) {
        var key;
        if ( typeof obj === "string" ) {
            // base case
            paths.push( path.concat( obj ) );
        } else if ( obj instanceof Array ) {
            // array, TODO
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

    // iterate over spec, and then for
    function applyObject( paths, bins ) {
        var agg = JSON.parse( JSON.stringify( bins[0] ) ),
            operator,
            aggItr,
            binItr,
            path,
            bin,
            key,
            value,
            i, j;
        for ( i=1; i<bins.length; i++ ) {
            bin = bins[i];
            for ( j=0; j<paths.length; j++ ) {
                binItr = bin;
                aggItr = agg;
                path = paths[j].slice( 0 );
                while ( path.length > 2 ) {
                    key = path.shift();
                    aggItr = aggItr[ key ];
                    binItr = binItr[ key ];
                }

                key = path.shift();

                switch ( operator ) {

                    case "$sum":

                        aggItr[ key ] = sum( aggItr[ key ], binItr[ key ] );
                        break;
                    case "$append":

                        aggItr[ key ] = append( aggItr[ key ], binItr[ key ] );
                        break;

                    case "$max":

                        break;

                    case "min":

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
    function BucketOperation( spec ) {
        this.spec = spec || {};
    }

    BucketOperation.prototype.apply = function( bins ) {

    };

    module.exports = BucketOperation;
}());
