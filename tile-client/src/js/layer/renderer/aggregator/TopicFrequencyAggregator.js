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

    var Aggregator = require('./Aggregator'),
        Util = require('../../../util/Util');

    /**
     * Instantiate a TopicFrequencyAggregator object.
     * @class TopicFrequencyAggregator
     * @classdesc
     */
    function TopicFrequencyAggregator() {
    }

    TopicFrequencyAggregator.prototype = Object.create( Aggregator.prototype );

    /**
     * Given an array of buckets, will execute the provided aggregation
     * specification against all relevant entries.
     * @memberof TopicFrequencyAggregator
     *
     * @param {Array} buckets - The array of buckets.
     *
     * @returns {Array} The aggregated buckets.
     */
    TopicFrequencyAggregator.prototype.aggregate = function( buckets ) {
        var that = this,
            indexByTopic = {},
            range = this.getBucketRange( buckets ),
            bucketCount = range.end - range.start + 1,
            topics = [];
        this.forEach(
            buckets,
            function( bucket, index ) {
                var topicIndex,
                    topic,
                    score,
                    total,
                    i, j,
                    val;
                if ( bucket ) {
                    for ( i=0; i<bucket.length; i++ ) {
                        topic = bucket[i].topic;
                        if ( indexByTopic[ topic ] === undefined ) {
                            // create index if it does not exist
                            indexByTopic[ topic ] = topics.length;
                            topics.push({
                                topic: topic,
                                topicEnglish: that.translateTopic( topic ),
                                count: 0,
                                frequencies: Util.fillArray( bucketCount )
                            });
                        }
                        score = bucket[i].score;
                        total = ( typeof score === "number" || score instanceof Array ) ? score : score.total;
                        topicIndex = indexByTopic[ topic ];

                        if (total instanceof Array) {
                            // Calculate total aggregate and indexed aggregate
                            if (!topics[ topicIndex ].indexedCount) {
                                topics[topicIndex].indexedCount = new Array(total.length).fill(0);
                                topics[topicIndex].indexedFrequencies = new Array(bucketCount);
                            }
                            if (!topics[topicIndex].indexedFrequencies[index]) {
                                topics[topicIndex].indexedFrequencies[index] = new Array(total.length).fill(0);
                            }

                            var summedTotal = 0;

                            for (j = 0; j < total.length; j++) {
                                val = total[j];
                                summedTotal += val;
                                topics[topicIndex].indexedCount[j] += val;
                                topics[topicIndex].indexedFrequencies[index][j] += val;
                            }
                            total = summedTotal;
                        }

                        topics[ topicIndex ].count += total;
                        topics[ topicIndex ].frequencies[ index ] = total;
                    }
                }
        });
        // sort topics based on count
        topics.sort( function( a, b ) {
            return b.count - a.count;
        });
        return topics;
    };

    module.exports = TopicFrequencyAggregator;

}());
