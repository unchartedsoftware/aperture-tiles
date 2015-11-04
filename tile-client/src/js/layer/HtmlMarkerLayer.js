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

(function() {

	"use strict";

	var Layer = require('./Layer'),
		PubSub = require('../util/PubSub');

	function addMarkerToLayer( layer, marker ) {
		marker.layer = layer;
		marker.map = layer.map;
		marker.activate();
	}

	function removeMarkerFromLayer( marker ) {
		marker.deactivate();
		marker.map = null;
		marker.layer = null;
	}

	/**
	 * Instantiate a HtmlMarkerLayer object.
	 * @class HtmlMarkerLayer
	 * @augments Layer
	 * @classdesc A client rendered marker layer object.
	 *
	 * @param {Object} spec - The specification object.
	 */
	function HtmlMarkerLayer( spec ) {
		spec = spec || {};
		// call base constructor
		Layer.call( this, spec );
		// set reasonable defaults
		this.zIndex = ( spec.zIndex !== undefined ) ? spec.zIndex : 749;
		this.domain = "marker";
		this.source = spec.source;
		this.markers = spec.markers || [];
	}

	HtmlMarkerLayer.prototype = Object.create( Layer.prototype );

	/**
	 * Activates the layer object. This should never be called manually.
	 * @memberof HtmlMarkerLayer
	 * @private
	 */
	HtmlMarkerLayer.prototype.activate = function() {
		this.olLayer = new OpenLayers.Layer.Markers( "Markers" );
		this.setOpacity( this.opacity );
		this.setEnabled( this.enabled );
		this.setTheme( this.map.getTheme() );
		this.map.olMap.addLayer( this.olLayer );
		this.markers.forEach( function( marker ) {
			addMarkerToLayer( this, marker );
		}, this );
		this.setZIndex( this.zIndex );
		PubSub.publish( this.getChannel(), { field: 'activate', value: true } );
	};

	/**
	 * Dectivates the layer object. This should never be called manually.
	 * @memberof HtmlMarkerLayer
	 * @private
	 */
	HtmlMarkerLayer.prototype.deactivate = function() {
		if ( this.olLayer ) {
			this.olLayer.clearMarkers();
			this.map.olMap.removeLayer( this.olLayer );
			this.olLayer.destroy();
			this.olLayer = null;
			PubSub.publish( this.getChannel(), { field: 'deactivate', value: true } );
		}
	};

	HtmlMarkerLayer.prototype.addMarker = function( marker ) {
		if ( this.olLayer ) {
			// add marker
			addMarkerToLayer( this, marker );
		}
		this.markers.push( marker );
	};

	HtmlMarkerLayer.prototype.addMarkers = function( markers, chunkSize, pause ) {
		var that = this;
		if ( chunkSize ) {
			// adding large quantities of markers to the map is slow, so
			// break it into async chunks to let the app breath in between
			_.chunk( markers, chunkSize ).forEach( function( chunk, index ) {
				setTimeout( function() {
					chunk.forEach( function( marker ) {
						that.addMarker( marker );
					});
				}, pause * index || 0 );
			});
		} else {
			markers.forEach( function( marker ) {
				that.addMarker( marker );
			});
		}
	};

	HtmlMarkerLayer.prototype.disableMarkers = function() {
		this.markers.forEach( function( marker ) {
			marker.disable();
		});
	};

	HtmlMarkerLayer.prototype.removeMarker = function( marker ) {
		if ( this.olLayer ) {
			removeMarkerFromLayer( marker );
		}
	};

	HtmlMarkerLayer.prototype.clearMarkers = function() {
		if ( this.olLayer ) {
			this.disableMarkers();
			this.olLayer.clearMarkers();
		}
		this.markers = [];
	};

	/**
	 * Updates the theme associated with the layer.
	 * @memberof HtmlMarkerLayer
	 *
	 * @param {String} theme - The theme identifier string.
	 */
	HtmlMarkerLayer.prototype.setTheme = function( theme ) {
		this.theme = theme;
	};

	/**
	 * Set the z index of the layer.
	 * @memberof HtmlMarkerLayer
	 *
	 * @param {integer} zIndex - The new z-order value of the layer, where 0 is front.
	 */
	HtmlMarkerLayer.prototype.setZIndex = function( zIndex ) {
		// we by-pass the OpenLayers.Map.setLayerIndex() method and manually
		// set the z-index of the layer dev. setLayerIndex sets a relative
		// index based on current map layers, which then sets a z-index. This
		// caused issues with async layer loading.
		this.zIndex = zIndex;
		if ( this.olLayer ) {
			$( this.olLayer.div ).css( 'z-index', zIndex );
			PubSub.publish( this.getChannel(), {
				field: 'zIndex',
				value: zIndex
			});
		}
	};

	/**
	 * Get the layers zIndex.
	 * @memberof HtmlMarkerLayer
	 *
	 * @returns {integer} The zIndex for the layer.
	 */
	HtmlMarkerLayer.prototype.getZIndex = function() {
		return this.zIndex;
	};

	module.exports = HtmlMarkerLayer;
}());
