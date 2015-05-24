/*
 * Copyright © 2013-2015 Uncharted Software Inc.
 *
 * Property of Uncharted™, formerly Oculus Info Inc.
 * http://uncharted.software/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
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

    var ClientLayer = require('./ClientLayer'),
        PubSub = require('../util/PubSub');

    function getURLFunc( layers ) {
        return function( bounds ) {
            return layers.map( function( layer ) {
                return layer.olLayer.getURL( bounds );
            });
        };
    }

    function MultiUrlClientLayer(spec) {
        ClientLayer.call( this, spec );
        this.getURL = getURLFunc( spec.source.layers );
    }

    MultiUrlClientLayer.prototype = Object.create( ClientLayer.prototype );

    // CDB: Not working properly yet.
    MultiUrlClientLayer.prototype.setLevelMinMax = function() {
        var zoomLevel = this.map.getZoom(),
            renderer = this.renderer;
        // apply this aggregator to child laeyrs
        var levelMinMax = this.source.layers.map( function( layer ) {
            var source = layer.source,
                meta = source.meta && source.meta.meta ? source.meta.meta[ zoomLevel ] : null,
                transformData = layer.tileTransform.data || {},
                levelMinMax = meta,
                aggregated;
            if ( meta ) {
                // aggregate the data if there is an aggregator attached
                if ( renderer && renderer.aggregator ) {
                    // aggregate the meta data buckets
                    aggregated = renderer.aggregator.aggregate(
                        meta.bins || [],
                        transformData.startBucket,
                        transformData.endBucket );
                    if ( aggregated instanceof Array ) {
                        // take the first and last index, which correspond to max / min
                        levelMinMax = {
                            minimum: aggregated[aggregated.length - 1],
                            maximum: aggregated[0]
                        };
                    } else {
                        //
                        levelMinMax = {
                            minimum: aggregated,
                            maximum: aggregated
                        };
                    }
                }
            } else {
                levelMinMax = {
                    minimum: null,
                    maximum: null
                };
            }
            return levelMinMax;
        });
        this.levelMinMax = levelMinMax;
        PubSub.publish( this.getChannel(), { field: 'levelMinMax', value: levelMinMax });
    };

    module.exports = MultiUrlClientLayer;
}());
