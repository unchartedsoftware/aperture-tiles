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
		Util = require('../util/Util'),
		HtmlTileLayer = require('./HtmlTileLayer'),
		PubSub = require('../util/PubSub');

	function getURL( bounds, ids ) {
		var tileIndex = LayerUtil.getTileIndex( this, bounds ),
			x = tileIndex.xIndex,
			y = tileIndex.yIndex,
			z = tileIndex.level;
		var result = _.map(ids, function(id) {
				if ( x >= 0 && y >= 0 ) {
					return this.url + id + "/" + z + "/" + x + "/" + y + "." + this.type;
				}
		}, this);
		return result;
	}

	function MultiUrlClientLayer(spec) {
		var that = this;

		ClientLayer.call(this, spec);
		this.getURL = function(bounds) {
			return _.map(getURL.call(this, bounds, spec.source.layers), function(url) {
				return url + that.getQueryParamString()
			});
		}
	}

	MultiUrlClientLayer.prototype = Object.create( ClientLayer.prototype );

	/**
	 * Activates the layer object. This should never be called manually.
	 * @memberof ClientLayer
	 * @private
	 */
	MultiUrlClientLayer.prototype.activate = function() {
		ClientLayer.prototype.activate.call(this);
	};

	/**
	 * Dectivates the layer object. This should never be called manually.
	 * @memberof ClientLayer
	 * @private
	 */
	MultiUrlClientLayer.prototype.deactivate = function() {
		ClientLayer.prototype.deactivate.call(this);
	};

	module.exports = MultiUrlClientLayer
}());

