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
		this.id = spec.id;
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
			var units = kml.units;

			if (typeof units === "object") {
				units = units[kml.url.split("/").pop()];
			}

			switch ( units ) {
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
				default:
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
    	// publish activate event before appending to map
        PubSub.publish( this.getChannel(), { field: 'activate', value: true } );
		this.olLayers.forEach( function( olLayer ) {
			this.map.olMap.addLayer( olLayer );
		}, this );
		this.setZIndex( this.zIndex );
        // publish add event
        PubSub.publish( this.getChannel(), { field: 'add', value: true } );
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
            PubSub.publish( this.getChannel(), { field: 'remove', value: true } );
			this.kml.forEach( function( kml ) {
				kml.olLayer = null;
			});
			this.olLayers = [];
		}
        PubSub.publish( this.getChannel(), { field: 'deactivate', value: true } );
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

	KMLLayer.prototype.setTileTransformRange = function( start ) {
		var kmlDate = start;
		if (kmlDate >= this.source.meta.meta.rangeMax) {
			kmlDate = this.source.meta.meta.rangeMax;
		} else if (kmlDate <= this.source.meta.meta.rangeMin) {
			kmlDate = this.source.meta.meta.rangeMin;
		}
		if ( kmlDate !== this.kmlDate ) {
			this.kmlDate = kmlDate;
			this.updateKMLData( true );
		}
	};

	KMLLayer.prototype.setTileTransformData = function() {
		// Set kml data to the most recent
		if ( this.source.meta.meta.rangeMax !== this.kmlDate ) {
			this.kmlDate = this.source.meta.meta.rangeMax;
			this.updateKMLData(true);
		}
	};

    KMLLayer.prototype.updateKMLData = function (updateView) {
	    var self = this;
	    var date = this.kmlDate;
	    if (updateView) {
	        this.deactivate();
	    }
	    this.kml.forEach( function( kml, kmlIndex ) {
	        if (kml.files) {
		        // Find closest month before
		        var smallestFile = null;
		        var minDiff;

		        kml.files.forEach(function (file) {
		            if (!smallestFile || Math.abs(file.date - date) < minDiff) {
		                minDiff = Math.abs(file.date - date);
		                smallestFile = file;
		            }
		        });
		        self.name = self.source.name + " (" + moment(smallestFile.date).format("MMM YYYY") + ")";

		        if (smallestFile) {
		            kml.url = "rest/layers/" + self.id + "/kml/" + kmlIndex + "/" + smallestFile.fileName;
		        }
	        }
	    });
	    if (updateView) {
			this.activate();
		}
    };

	module.exports = KMLLayer;
}());
