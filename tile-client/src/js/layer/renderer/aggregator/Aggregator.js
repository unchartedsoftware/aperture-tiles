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

    /**
     * Instantiate a Aggregator object.
     * @class Aggregator
     * @classdesc
     */
    function Aggregator() {
    }

    /**
     * Given an array of buckets, will execute the provided aggregation
     * specification against all relevant entries.
     * @memberof Aggregator
     *
     * @param {Array} buckets - The array of buckets.
     *
     * @returns {Array} The aggregated buckets.
     */
    Aggregator.prototype.aggregate = function( buckets ) {
        return buckets;
    };

    /**
     * Attaches the aggregator to its respective layer. This method should not be called
     * manually.
     * @memberof Aggregator
     * @private
     *
     * @param {Layer} layer - The layer to attach to the renderer.
     */
     Aggregator.prototype.attach = function( layer ) {
        if ( this.parent && this.parent !== layer ) {
            console.log( "This renderer has already been attached " +
                         "to a different layer, please use another instance." );
            return;
        }
        this.parent = layer;
    };

    /**
     * Returns the start and end indices for the set of buckets.
     * @memberof Aggregator
     *
     * @param {Array} buckets - The array of buckets.
     *
     * @returns {Object} The range object.
     */
    Aggregator.prototype.getBucketRange = function( buckets ) {
        var tileTransformData = this.parent.getTileTransformData(),
            start = tileTransformData.startBucket,
            end = tileTransformData.endBucket;
        return {
            start: start !== undefined ? start : 0,
            end: end !== undefined ? end : buckets.length - 1
        };
    };

    /**
     * Checks the layer metadata for a translation map. If it exists, returns
     * the translated entry for the provided topic.
     * @memberof Aggregator
     *
     * @param {String} topic - The topic to translate.
     *
     * @returns {String} The translated topic.
     */
    Aggregator.prototype.translateTopic = function( topic ) {
        var meta = this.parent.source.meta.meta;
        if ( meta.translatedTopics ) {
            return meta.translatedTopics[ topic ];
        }
        return undefined;
    };

    /**
     * Executes a function for each bucket, passing the bucket and offset
     * reduced index arguments.
     * @memberof Aggregator
     *
     * @param {Array} buckets - The array of buckets.
     * @param {Function} func - The function to execute.
     */
    Aggregator.prototype.forEach = function( buckets, func ) {
        var range = this.getBucketRange( buckets ),
            start = range.start,
            end = range.end,
            i;
        // first iterate over all buckets and organize them by id
        for ( i=start; i<=end; i++ ) {
            // subtract start to always have index 0 based
            if ( func( buckets[i], i-start ) ) {
                return;
            }
        }
    };

    module.exports = Aggregator;

}());
