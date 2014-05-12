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

/* JSLint global declarations: these objects don't need to be declared. */
/*global OpenLayers */



define(function (require) {
	"use strict";


	
	var Class = require('../class'),
	    AoIPyramid = require('../binning/AoITilePyramid'),
	    PyramidFactory = require('../binning/PyramidFactory'),
	    TileIterator = require('../binning/TileIterator'),
	    Axis =  require('./Axis'),
	    TILESIZE = 256,
	    Map;



	Map = Class.extend({
		ClassName: "Map",
		
		init: function (id, spec) {

			var that = this,
			    mapSpecs;
			
			mapSpecs = spec.MapConfig;

			aperture.config.provide({
				// Set the map configuration
				'aperture.map' : {
					'defaultMapConfig' : mapSpecs
				}
			});
			
			
			// Map div id
			this.id = id;

			// Initialize the map
			this.map = new aperture.geo.Map({ 
				id: this.id
			});

			/*
			 this.tileManager = new OpenLayers.TileManager();
			 this.tileManager.addMap(this.map.olMap_);
			 */

			this.map.olMap_.baseLayer.setOpacity(1);
			this.map.all().redraw();
			this.axes = [];

			this.pyramid = PyramidFactory.createPyramid(spec.PyramidConfig);

			// Set resize map callback
			$(window).resize( function() {
				// set map to full extent of window
				var $map = $('#' + that.id);
				$map.width( $(window).width() );
				$map.height( $(window).height() );
				that.updateSize();
				that.redrawAxes();
			});

			this.previousZoom = this.map.getZoom();

			// Trigger the initial resize event to resize everything
			$(window).resize();			
		},


        getElement:  function() {
            return $("#" + this.id);
        },

		setAxisSpecs: function (axes) {

			var i, spec;

			for (i=0; i< axes.length; i++) {
				spec = axes[i];
				spec.mapId = this.id;
				spec.map = this;
				this.axes.push(new Axis(spec));
			}
		},


		redrawAxes: function() {
			var i;
			for (i=0; i<this.axes.length; i++) {
				this.axes[i].redraw();
			}
		},


		getAxes: function() {
			return this.axes;
		},


		getPyramid: function() {

			return this.pyramid;
		},


		getTileIterator: function() {
			var level = this.map.getZoom(),
			    // Current map bounds, in meters
			    bounds = this.map.olMap_.getExtent(),
			    // Total map bounds, in meters
			    mapExtent = this.map.olMap_.getMaxExtent(),
			    // Pyramider for the total map bounds
			    mapPyramid = new AoIPyramid(mapExtent.left, mapExtent.bottom,
			                                mapExtent.right, mapExtent.top);

			// determine all tiles in view
			return new TileIterator( mapPyramid, level,
			                         bounds.left, bounds.bottom,
			                         bounds.right, bounds.top);
		},


		getTilesInView: function() {

			return this.getTileIterator().getRest();
		},


		getTileSetBoundsInView: function() {

			return {'params': this.getTileIterator().toTileBounds()};
		},


		getProjectioin: function() {
			return this.map.olMap_.projection;
		},

		getViewportWidth: function() {
			return this.map.olMap_.viewPortDiv.clientWidth;
		},


		getViewportHeight: function() {
			return this.map.olMap_.viewPortDiv.clientHeight;
		},

		/**
		 * Returns the min and max visible viewport pixels
		 * Axes may be covering parts of the map, so this determines the actual visible
		 * bounds
		 */
		getMinMaxVisibleViewportPixels: function() {

			var bounds ={
				min : {
					x: 0,
					y: 0
				},
				max : {
					x: this.getViewportWidth(),
					y: this.getViewportHeight()
				}
			}, i;

			// determine which axes exist
			for (i=0; i<this.axes.length; i++) {

				if (this.axes[i].isEnabled()) {

					switch ( this.axes[i].position ) {

					case 'top':
						bounds.min.y = this.axes[i].getMaxContainerWidth();
						break;
					case 'bottom':
						bounds.max.y = this.getViewportHeight() - this.axes[i].getMaxContainerWidth();
						break;
					case 'left':
						bounds.min.x = this.axes[i].getMaxContainerWidth();
						break;
					case 'right':
						bounds.max.x = this.getViewportWidth() - this.axes[i].getMaxContainerWidth();
						break;
					}
				}
			}

			return bounds;
		},


		/**
		 * Returns the maps min and max pixels in viewport pixels
		 * NOTE:    viewport [0,0] is TOP-LEFT
		 *          map [0,0] is BOTTOM-LEFT
		 */
		getMapMinAndMaxInViewportPixels: function() {
			return {
				min : {
					x: this.map.olMap_.minPx.x,
					y: this.map.olMap_.maxPx.y
				},
				max : {
					x: this.map.olMap_.maxPx.x,
					y: this.map.olMap_.minPx.y
				}
			};
		},


		/**
		 * Transforms a point from viewport pixel coordinates to map pixel coordinates
		 * NOTE:    viewport [0,0] is TOP-LEFT
		 *          map [0,0] is BOTTOM-LEFT
		 */
		getMapPixelFromViewportPixel: function(vx, vy) {
			var viewportMinMax = this.getMapMinAndMaxInViewportPixels(),
			    totalPixelSpan = TILESIZE * Math.pow( 2, this.getZoom() );
			return {
				x: totalPixelSpan + vx - viewportMinMax.max.x,
				y: totalPixelSpan - vy + viewportMinMax.max.y
			};
		},


		/**
		 * Transforms a point from map pixel coordinates to viewport pixel coordinates
		 * NOTE:    viewport [0,0] is TOP-LEFT
		 *          map [0,0] is BOTTOM-LEFT
		 */
		getViewportPixelFromMapPixel: function(mx, my) {
			var viewportMinMax = this.getMapMinAndMaxInViewportPixels(),
			    totalPixelSpan = TILESIZE * Math.pow( 2, this.getZoom() );
			return {
				x: mx + viewportMinMax.min.x,
				y: totalPixelSpan - my + viewportMinMax.max.y
			};
		},


		/**
		 * Transforms a point from data coordinates to map pixel coordinates
		 * NOTE:    data and map [0,0] are both BOTTOM-LEFT
		 */
		getMapPixelFromCoord: function(x, y) {
			var zoom = this.map.olMap_.getZoom(),
			    tile = this.pyramid.rootToTile( x, y, zoom, TILESIZE),
			    bin = this.pyramid.rootToBin( x, y, tile);
			return {
				x: tile.xIndex * TILESIZE + bin.x,
				y: tile.yIndex * TILESIZE + TILESIZE - 1 - bin.y
			};
		},


		/**
		 * Transforms a point from map pixel coordinates to data coordinates
		 * NOTE:    data and map [0,0] are both BOTTOM-LEFT
		 */
		getCoordFromMapPixel: function(mx, my) {
			var tileAndBin = this.getTileAndBinFromMapPixel(mx, my, TILESIZE, TILESIZE),
			    bounds = this.pyramid.getBinBounds( tileAndBin.tile, tileAndBin.bin );
			return {
				x: bounds.minX,
				y: bounds.minY
			};
		},


		/**
		 * Transforms a point from viewport pixel coordinates to data coordinates
		 * NOTE:    viewport [0,0] is TOP-LEFT
		 *          data [0,0] is BOTTOM-LEFT
		 */
		getCoordFromViewportPixel: function(vx, vy) {
			var mapPixel = this.getMapPixelFromViewportPixel(vx, vy);
			return this.getCoordFromMapPixel(mapPixel.x, mapPixel.y);
		},

		/**
		 * Transforms a point from data coordinates to viewport pixel coordinates
		 * NOTE:    viewport [0,0] is TOP-LEFT
		 *          data [0,0] is BOTTOM-LEFT
		 */
		getViewportPixelFromCoord: function(x, y) {
			var mapPixel = this.getMapPixelFromCoord(x, y);
			return this.getViewportPixelFromMapPixel(mapPixel.x, mapPixel.y);
		},


		/**
         * Returns the tile and bin index corresponding to the given map pixel coordinate
         */
        getTileAndBinFromMapPixel: function(mx, my, xBinCount, yBinCount) {
            var tileIndexX = Math.floor(mx / TILESIZE),
                tileIndexY = Math.floor(my / TILESIZE),
                tilePixelX = mx % TILESIZE,
                tilePixelY = my % TILESIZE;
            return {
                tile: {
                    level : this.getZoom(),
                    xIndex : tileIndexX,
                    yIndex : tileIndexY,
                    xBinCount : xBinCount,
                    yBinCount : yBinCount
                },
                bin: {
                    x : Math.floor( tilePixelX / (TILESIZE / xBinCount) ),
                    y : (yBinCount - 1) - Math.floor( tilePixelY / (TILESIZE / yBinCount) ) // bin [0,0] is top left
                }
            };
        },


        /**
         * Returns the top left pixel location in viewport coord from a tile index
         */
        getTopLeftViewportPixelForTile: function(tx, ty) {

            var mx = tx * TILESIZE,
                my = ty * TILESIZE + TILESIZE,
                pixel;

            // transform map coord to viewport coord
            pixel = this.getViewportPixelFromMapPixel(mx, my);

            return {
                x : Math.round(pixel.x),
                y : Math.round(pixel.y)
            };
        },


		/**
		 * Returns the tile and bin index corresponding to the given viewport pixel coordinate
		 */
		getTileAndBinFromViewportPixel: function(vx, vy, xBinCount, yBinCount) {
			var mapPixel = this.getMapPixelFromViewportPixel(vx, vy);
			return this.getTileAndBinFromMapPixel( mapPixel.x, mapPixel.y, xBinCount, yBinCount );
		},


		/**
		 * Returns the tile and bin index corresponding to the given data coordinate
		 */
		getTileAndBinFromCoord: function(x, y, xBinCount, yBinCount) {
			var mapPixel = this.getMapPixelFromCoord(x, y);
			return this.getTileAndBinFromMapPixel( mapPixel.x, mapPixel.y, xBinCount, yBinCount );
		},


		getTileKeyFromViewportPixel: function(vx, vy) {
			var tileAndBin = this.getTileAndBinFromViewportPixel(vx, vy, 1, 1);
			return tileAndBin.tile.level + "," + tileAndBin.tile.xIndex + "," + tileAndBin.tile.yIndex;
		},


		getBinKeyFromViewportPixel: function(vx, vy, xBinCount, yBinCount) {
			var tileAndBin = this.getTileAndBinFromViewportPixel( vx, vy, xBinCount, yBinCount );
			return tileAndBin.bin.x + "," + tileAndBin.bin.y;
		},


		getCoordFromMap: function (x, y) {
			var
			// Total map bounds, in meters
			mapExtent = this.map.olMap_.getMaxExtent(),
			// Pyramid for the total map bounds
			mapPyramid = new AoIPyramid(mapExtent.left, mapExtent.bottom,
			                            mapExtent.right, mapExtent.top),
			tile = mapPyramid.rootToFractionalTile({level: 0, xIndex: x, yIndex: y}),
			coords = this.pyramid.fractionalTileToRoot(tile);
			return {x: coords.xIndex, y: coords.yIndex};
		},

		transformOLGeometryToLonLat: function (geometry) {
			return geometry.transform(new OpenLayers.projection("EPSG:900913"),
			                          new OpenLayers.projection("EPSG:4326"));
		},

		getOLMap: function() {
			return this.map.olMap_;
		},

		getApertureMap: function() {
			return this.map;
		},

		addApertureLayer: function(layer, mappings, spec) {
			return this.map.addLayer(layer, mappings, spec);
		},

		addOLLayer: function(layer) {
			return this.map.olMap_.addLayer(layer);
		},

		addOLControl: function(control) {
			return this.map.olMap_.addControl(control);
		},

		getUid: function() {
			return this.map.uid;
		},

		setLayerIndex: function(layer, zIndex) {
			this.map.olMap_.setLayerIndex(layer, zIndex);
		},

		getLayerIndex: function(layer) {
			return this.map.olMap_.getLayerIndex(layer);
		},

		setOpacity: function (newOpacity) {
			this.map.olMap_.baseLayer.setOpacity(newOpacity);
		},

		getOpacity: function () {
			return this.map.olMap_.baseLayer.opacity;
		},

		setVisibility: function (visibility) {
			this.map.olMap_.baseLayer.setVisibility(visibility);
		},

		getExtent: function () {
			return this.map.olMap_.getExtent();
		},

		getZoom: function () {
			return this.map.olMap_.getZoom();
		},

		isEnabled: function () {
			return this.map.olMap_.baseLayer.getVisibility();
		},

		setEnabled: function (enabled) {
			this.map.olMap_.baseLayer.setVisibility(enabled);
		},

		zoomToExtent: function (extent, findClosestZoomLvl) {
			this.map.olMap_.zoomToExtent(extent, findClosestZoomLvl);
		},


		updateSize: function() {
			this.map.olMap_.updateSize();
		},


		getTileSize: function() {
			return TILESIZE;
		},


		on: function (eventType, callback) {

			var that = this;

			switch (eventType) {

			case 'click':
			case 'zoomend':
			case 'mousemove':
			case 'moveend':
            case 'move':
				this.map.olMap_.events.register(eventType, this.map.olMap_, callback );
                break;

			case 'panend':

				/*
				 * ApertureJS 'panend' event is simply an alias to 'moveend' which also triggers
				 * on 'zoomend'. This intercepts it and ensures 'panend' is not called on a 'zoomend'
				 * event
				 */
				this.map.olMap_.events.register('moveend', this.map.olMap_, function(e) {

					if (that.previousZoom === e.object.zoom ) {
						// only process if zoom is same as previous
						callback();
					}
					that.previousZoom = e.object.zoom;
				});
				break;


			default:

				this.map.on(eventType, callback);
				break;
			}

		},

		off: function(eventType, callback) {

			switch (eventType) {

			case 'click':
			case 'zoomend':
			case 'mousemove':
			case 'moveend':
            case 'move':

				this.map.olMap_.events.unregister(eventType, this.map.olMap_, callback);
				break;

			case 'panend':

				if (callback !== undefined) {
					console.log("Error, cannot unregister specified 'panend' event");
				} else {
					this.map.olMap_.events.unregister(eventType, this.map.olMap_);
				}
				break;

			default:

				this.map.off(eventType, callback);
				break;
			}
		},

		trigger: function(eventType, event) {

			switch (eventType) {

			case 'click':
			case 'zoomend':
			case 'mousemove':
			case 'moveend':
            case 'move':

				this.map.olMap_.events.triggerEvent(eventType, event);
				break;

			default:

				this.map.trigger(eventType, event);
				break;
			}
		}

	});

	return Map;
});
