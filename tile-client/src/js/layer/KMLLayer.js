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

	/**
	 * Instantiate a KMLLayer object.
	 * @class KMLLayer
	 * @augments Layer
	 * @classdesc A client rendered layer object.
	 *
	 * @param {Object} spec - The specification object.
	 */
	function KMLLayer( spec ) {
		// call base constructor
		Layer.call(this, spec);
		// set reasonable defaults
		this.zIndex = ( spec.zIndex !== undefined ) ? spec.zIndex : 749;
		this.domain = "kml";
		this.source = spec.source;
		this.kml = spec.kml || [];
	}

	KMLLayer.prototype = Object.create(Layer.prototype);

	/**
	 * Activates the layer object. This should never be called manually.
	 * @memberof KMLLayer
	 * @private
	 */
	KMLLayer.prototype.activate = function() {
		this.olLayers = [];

		this.kml.forEach( function( kml ) {
			var projection;
			switch ( kml.units ) {
				case "meter":
				case "meters":
				case "metres":
				case "metre":
				case "m":
					projection = new OpenLayers.Projection("EPSG:900913");
					break;
				case "degrees":
				case "degree":
					projection = new OpenLayers.Projection("EPSG:4326");
					break;
				case "default":
					projection = new OpenLayers.Projection("EPSG:4326");
					break;
			}
			kml.olLayer = new OpenLayers.Layer.Vector( "Vector Layer", {
				projection: projection,
				strategies: [
					new OpenLayers.Strategy.Fixed()
				],
                protocol: new OpenLayers.Protocol.HTTP({
                    url: kml.url,
                    format: new OpenLayers.Format.KML({
                        extractStyles: true,
                        extractAttributes: true
                    })
                })
			});
			this.olLayers.push( kml.olLayer );
		}, this );

        this.setEnabled( this.isEnabled() );
        this.setOpacity( this.getOpacity() );
        this.setBrightness( this.getBrightness() );
        this.setContrast( this.getContrast() );

		this.olLayers.forEach( function( olLayer ) {
			this.map.olMap.addLayer( olLayer );
		}, this );

		this.setZIndex( this.zIndex );
	};

	/**
	 * Dectivates the layer object. This should never be called manually.
	 * @memberof KMLLayer
	 * @private
	 */
	KMLLayer.prototype.deactivate = function() {
		if ( this.olLayers ) {
			this.olLayers.forEach( function( olLayer ) {
				this.map.olMap.removeLayer( olLayer );
				olLayer.destroy();
				olLayer = null;
			}, this );
			this.kml.forEach( function( kml ) {
				kml.olLayer = null;
			});
			this.olLayers = [];
		}
	};

	/**
	 * Set the z index of the layer.
	 * @memberof KMLLayer
	 *
	 * @param {integer} zIndex - The new z-order value of the layer, where 0 is front.
	 */
	KMLLayer.prototype.setZIndex = function( zIndex ) {
		// we by-pass the OpenLayers.Map.setLayerIndex() method and manually
		// set the z-index of the layer dev. setLayerIndex sets a relative
		// index based on current map layers, which then sets a z-index. This
		// caused issues with async layer loading.
		this.zIndex = zIndex;
		if ( this.olLayers ) {
			this.olLayers.forEach( function( olLayer, index ) {
				$( olLayer.div ).css( 'z-index', zIndex + ( this.olLayers.length - index ) );
			}, this );
		}
		PubSub.publish( this.getChannel(), {
			field: 'zIndex',
			value: zIndex
		});
	};

	/**
	 * Get the layers zIndex.
	 * @memberof KMLLayer
	 *
	 * @returns {integer} The zIndex for the layer.
	 */
	KMLLayer.prototype.getZIndex = function() {
		return this.zIndex;
	};

	/**
	* Set the opacity of the layer.
	* @memberof KMLLayer
	*
	* @param {float} opacity - opacity value from 0 to 1.
	*/
	KMLLayer.prototype.setOpacity = function( opacity ) {
		this.opacity = opacity;
		if ( this.olLayers ) {
			this.olLayers.forEach( function( olLayer ) {
				olLayer.setOpacity( opacity );
			}, this );
		}
		PubSub.publish( this.getChannel(), { field: 'opacity', value: opacity } );
	};

	/**
	* Set whether or not the layer is enabled.
	* @memberof KMLLayer
	*
	* @param enabled {boolean} whether the layer is visible or not
	*/
	KMLLayer.prototype.setEnabled = function( enabled ) {
		this.enabled = enabled;
		if ( this.olLayers ) {
			this.olLayers.forEach( function( olLayer ) {
				olLayer.setVisibility( enabled );
			}, this );
		}
		PubSub.publish( this.getChannel(), { field: 'enabled', value: enabled } );
	};

	module.exports = KMLLayer;
}());
