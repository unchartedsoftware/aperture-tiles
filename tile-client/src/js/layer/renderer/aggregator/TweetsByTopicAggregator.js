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
     * Returns the sentiment id string based on the numerical value.
     *
     * @param {number} value - The sentiment value.
     *
     * @returns {String} The sentiment id.
     */
    function parseSentiment( value ) {
        if ( value === undefined ) {
            return undefined;
        }
        if ( value === -1 ) {
            return 'negative';
        } else if ( value === 1 ) {
            return 'positive';
        }
        return 'neutral';
    }

    /**
     * Iterates over each bucket, and perform the aggregation.
     *
     * @param {Array} paths - The array of paths.
     * @param {Array} buckets - The array of buckets.
     *
     * @param {Array} The aggregated buckets.
     */
    function aggregateBucket( buckets ) {
        var tweets = [],
            parsed,
            texts,
            bucket,
            i, j;
        for ( i=0; i<buckets.length; i++ ) {
            bucket = buckets[i];
            texts = bucket.score.texts;
            for ( j=0; j<texts.length; j++ ) {
                parsed = JSON.parse( texts[j].text );
                tweets.push({
                    user: parsed[0],
                    text: parsed[1],
                    sentiment: parseSentiment( parsed[2] ),
                    timestamp: texts[j].score
                });
            }
        }
        return tweets;
    }

    /**
     * Instantiate a TweetsByTopicAggregator object.
     * @class TweetsByTopicAggregator
     * @classdesc
     */
    function TweetsByTopicAggregator() {
    }

    TweetsByTopicAggregator.prototype = Object.create( Aggregator.prototype );

    /**
     * Given an array of buckets, will execute the provided aggregation
     * specification against all relevant entries.
     * @memberof TweetsByTopicAggregator
     *
     * @param {Array} buckets - The array of buckets.
     * @param {number} startBucket - The start bucket. Optional.
     * @param {number} endBucket - The end bucket. Optional.
     *
     * @returns {Array} The aggregated buckets.
     */
    TweetsByTopicAggregator.prototype.aggregate = function( buckets ) {
        var bucketsByTopic = {},
            tweetsByTopic = {},
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
                tweetsByTopic[ topic ] = aggregateBucket( bucketsByTopic[ topic ] );
            }
        }
        return tweetsByTopic;
    };

    module.exports = TweetsByTopicAggregator;

}());
