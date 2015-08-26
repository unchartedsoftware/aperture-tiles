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

    var Aggregator = require('./Aggregator');

    /**
     * Iterates over each bucket, and perform the aggregation.
     *
     * @param {Aggregator} aggregator - The aggregator object.
     * @param {Array} buckets - The array of buckets.
     *
     * @param {Array} The aggregated buckets.
     */
    function aggregateBucket( aggregator, buckets ) {
        var aggregation,
            score,
            total,
            i;
        // set base aggregator
        aggregation = {
            topic: buckets[0].topic,
            topicEnglish: aggregator.translateTopic( buckets[0].topic ),
            count: 0
        };
        // for each bucket of data
        for ( i=0; i<buckets.length; i++ ) {
            // add to total count
            score = buckets[i].score;
            if (score !== undefined) {
                total = ( typeof score === "number" ) ? score : score.total;
                aggregation.count += total;
            }
        }
        return aggregation;
    }

    /**
     * Instantiate a TopicCountAggregator object.
     * @class TopicCountAggregator
     * @classdesc
     */
    function TopicCountAggregator() {
    }

    TopicCountAggregator.prototype = Object.create( Aggregator.prototype );

    /**
     * Given an array of buckets, will execute the provided aggregation
     * specification against all relevant entries.
     * @memberof TopicCountAggregator
     *
     * @param {Array} buckets - The array of buckets.
     *
     * @returns {Array} The aggregated buckets.
     */
    TopicCountAggregator.prototype.aggregate = function( buckets ) {
        var bucketsByTopic = {},
            aggBuckets = [],
            topic;
        this.forEach(
            buckets,
            function( bucket ) {
                var topic,
                    i;
                if ( bucket ) {
                    for ( i=0; i<bucket.length; i++ ) {
                        topic = bucket[i].topic;
                        bucketsByTopic[ topic ] = bucketsByTopic[ topic ] || [];
                        bucketsByTopic[ topic ].push( bucket[i] );
                    }
                }
        });
        // then, for each id, aggregate the buckets
        for ( topic in bucketsByTopic ) {
            if ( bucketsByTopic.hasOwnProperty( topic ) ) {
                aggBuckets.push( aggregateBucket( this, bucketsByTopic[ topic ] ) );
            }
        }
        // finally, sort them based on count
        aggBuckets.sort( function( a, b ) {
            return b.count - a.count;
        });
        return aggBuckets;
    };

    module.exports = TopicCountAggregator;

}());
