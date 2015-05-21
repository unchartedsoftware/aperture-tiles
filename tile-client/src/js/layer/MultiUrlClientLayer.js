/*
 * Copyright (c) 2014 Oculus Info Inc.
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

	var ClientLayer = require('./ClientLayer'),
		LayerUtil = require('./LayerUtil'),
		PubSub = require('../util/PubSub');

	function getURL( bounds, layers ) {
		var tileIndex = LayerUtil.getTileIndex( this, bounds ),
			x = tileIndex.xIndex,
			y = tileIndex.yIndex,
			z = tileIndex.level;
		var result = _.map(layers, function(layer) {
				if ( x >= 0 && y >= 0 ) {
					return this.url + layer.id + "/" + z + "/" + x + "/" + y + "." + this.type;
				}
		}, this);
		return result;
	}

	function MultiUrlClientLayer(spec) {
		var that = this;

		ClientLayer.call(this, spec);
		this.getURL = function(bounds) {
			return _.map(getURL.call(this, bounds, spec.source.layers), function(url) {
				return url + that.getQueryParamString();
			});
		};
	}

	MultiUrlClientLayer.prototype = Object.create( ClientLayer.prototype );

	MultiUrlClientLayer.prototype.setLevelMinMax = function(layer) {
		// Apply the aggregator across each level to get their min max.
		var zoomLevel = layer.map.getZoom(),
			meta =  layer.source.meta.meta[ zoomLevel ],
			transformData = layer.tileTransform.data || {},
			levelMinMax = meta,
			renderer = layer.renderer;

		// aggregate the data if there is an aggregator attached
		if ( renderer && renderer.aggregator ) {
			// aggregate the meta data buckets
			var aggregated = renderer.aggregator.aggregate(
				meta.bins,
				transformData.startBucket,
				transformData.endBucket );
			// take the first and last index, which correspond to max / min
			levelMinMax = {
				minimum: aggregated[aggregated.length - 1],
				maximum: aggregated[0]
			};
		}
		layer.levelMinMax = levelMinMax;
		PubSub.publish( layer.getChannel(), { field: 'levelMinMax', value: levelMinMax });
	};

	module.exports = MultiUrlClientLayer;
}());

