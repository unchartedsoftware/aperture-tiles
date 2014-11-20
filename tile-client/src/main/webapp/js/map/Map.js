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



	var BaseLayer = require('../layer/base/BaseLayer'),
        PubSub = require('../util/PubSub'),
	    AoIPyramid = require('../binning/AoITilePyramid'),
	    PyramidFactory = require('../binning/PyramidFactory'),
	    TileIterator = require('../binning/TileIterator'),
	    Axis =  require('./Axis'),
	    TILESIZE = 256;


	function Map( spec ) {

        var that = this;

        this.id = spec.id;
        this.$map = $( spec.selector || ("#" + this.id) );
        this.axes = [];
        this.pyramid = PyramidFactory.createPyramid( spec.pyramid );

        // Set the map configuration
        spec.baseLayer = {}; // default to no base layer
        aperture.config.provide({
            'aperture.map' : {
                'defaultMapConfig' : spec
            }
        });

        // initialize the map
        this.map = new aperture.geo.Map({
            id: this.id,
            options: {
                controls: [
                    new OpenLayers.Control.Navigation({ documentDrag: true }),
                    new OpenLayers.Control.Zoom()
                ]
            }
        });

        // create div root layer
        this.createRoot();

        // if mousedown while map is panning, interrupt pan
        this.getElement().mousedown( function(){
            if ( that.map.olMap_.panTween ) {
                 that.map.olMap_.panTween.callbacks = null;
                 that.map.olMap_.panTween.stop();
            }
        });

        // initialize previous zoom
        this.previousZoom = this.map.getZoom();
        this.baseLayerIndex = -1;

        this.setTileFocusCallbacks();

        // set resize callback
        $(window).resize( $.proxy(this.updateSize, this) );

        // Trigger the initial resize event to resize everything
        $(window).resize();

        if( spec.zoomTo ) {
            this.map.zoomTo( spec.zoomTo[0],
                             spec.zoomTo[1],
                             spec.zoomTo[2] );
        }
    }

    Map.prototype = {

        add: function( layer ) {
            if ( layer instanceof BaseLayer ) {
                this.baselayers = this.baselayers || [];
                this.baselayers.push( layer );
                if ( this.baseLayerIndex < 0 ) {
                    this.setBaseLayerIndex( 0 );
                }
            }
        },

		setTileFocusCallbacks: function() {
            var that = this,
                previousMouse = {};
            function updateTileFocus( layer, x, y ) {
                var tilekey = that.getTileKeyFromViewportPixel( x, y );
                if ( tilekey !== that.getTileFocus() ) {
                    // only update tilefocus if it actually changes
                    that.setTileFocus( tilekey );
                }
            }
		    // set tile focus callbacks
            this.on('mousemove', function(event) {
                updateTileFocus( that, event.xy.x, event.xy.y );
                previousMouse.x = event.xy.x;
                previousMouse.y = event.xy.y;
            });
            this.on('zoomend', function(event) {
                updateTileFocus( that, previousMouse.x, previousMouse.y );
            });
		},


		/**
         * Set the layers tile focus, identifying which tile the user is currently
         * hovering over.
         */
        setTileFocus: function( tilekey ) {
            this.previousTileFocus = this.tileFocus;
            this.tileFocus = tilekey;
            PubSub.publish( 'layer', { field: 'tileFocus', value: tilekey });
        },


        /**
         * Get the layers tile focus.
         */
        getTileFocus: function() {
            return this.tileFocus;
        },


        /**
         * Get the layers previous tile focus.
         */
        getPreviousTileFocus: function() {
            return this.previousTileFocus;
        },


        createRoot: function() {

            var that = this;
            this.$root = $('<div id="'+this.id+'-root" style="position:absolute; top:0px; z-index:999;"></div>');
            this.$map.append( this.$root );

            this.on('move', function() {
                var pos = that.getViewportPixelFromMapPixel( 0, that.getMapHeight() ),
                    translate = "translate("+ pos.x +"px, " + pos.y + "px)";
                that.$root.css({
                    "-webkit-transform": translate,
                    "-moz-transform": translate,
                    "-ms-transform": translate,
                    "-o-transform": translate,
                    "transform": translate
                });
                // update tiles in view
                that.updateTilesInView();
            });

            this.trigger('move'); // fire initial move event
        },


        setBaseLayerIndex: function( index ) {
            var oldBaseLayer = this.baselayers[ this.getPreviousBaseLayerIndex() ],
                newBaseLayer = this.baselayers[ index ];
            if ( oldBaseLayer ) {
                oldBaseLayer.deactivate();
            }
            newBaseLayer.activate();
            this.previousBaseLayerIndex = this.baseLayerIndex;
            this.baseLayerIndex = index;
            PubSub.publish( this.getChannel(), { field: 'baseLayerIndex', value: index });
        },

        getTheme: function() {
        	return $("body").hasClass("light-theme")? 'light' : 'dark';
        },

        getBaseLayerIndex: function() {
            return this.baseLayerIndex;
        },

        getPreviousBaseLayerIndex: function() {
            return this.previousBaseLayerIndex;
        },

        getElement:  function() {
            return this.$map;
        },

        getRootElement: function() {
            return this.$root;
        },


		setAxisSpecs: function ( axisConfig, pyramidConfig ) {

			var axes,
			    i;

			function parseAxisConfig( axisConfig, pyramidConfig ) {
                var i;
                if ( !axisConfig.boundsConfigured ) {
                    // bounds haven't been copied to the axes from the pyramid; copy them.
                    axisConfig.boundsConfigured = true;
                    for (i=0; i<axisConfig.length; i++) {
                        // TEMPORARY, eventually these bounds wont be needed, the axis will generate
                        // the increments solely based off the Map.pyramid
                        if ( pyramidConfig.type === "AreaOfInterest") {
                            if (axisConfig[i].position === 'top' || axisConfig[i].position === 'bottom' ) {
                                axisConfig[i].min = pyramidConfig.minX;
                                axisConfig[i].max = pyramidConfig.maxX;
                            } else {
                                axisConfig[i].min = pyramidConfig.minY;
                                axisConfig[i].max = pyramidConfig.maxY;
                            }
                        } else {
                            // web mercator
                            if (axisConfig[i].position === 'top' || axisConfig[i].position === 'bottom' ) {
                                axisConfig[i].min = -180.0;
                                axisConfig[i].max = 180.0;
                            } else {
                                axisConfig[i].min = -85.05;
                                axisConfig[i].max = 85.05;
                            }
                        }
                    }
                }
                return axisConfig;
            }

            // add bounds information if missing
            axes = parseAxisConfig( axisConfig, pyramidConfig );

			for (i=0; i< axes.length; i++) {
				axes[i].mapId = this.id;
				axes[i].map = this;
				this.axes.push( new Axis( axes[i] ) );
			}
			// set content dim after so that max out of all is used
            for (i=0; i< this.axes.length; i++) {
                this.axes[i].setContentDimension();
            }
            this.redrawAxes();
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
			    // Pyramid for the total map bounds
			    mapPyramid = new AoIPyramid(mapExtent.left, mapExtent.bottom,
			                                mapExtent.right, mapExtent.top);
			// determine all tiles in view
			return new TileIterator( mapPyramid, level,
			                         bounds.left, bounds.bottom,
			                         bounds.right, bounds.top);
		},


        updateTilesInView: function() {
            var tiles = this.getTileIterator().getRest(),
                culledTiles = [],
                maxTileIndex = Math.pow(2, this.getZoom() ),
                tile,
                i;
            for (i=0; i<tiles.length; i++) {
                tile = tiles[i];
                if ( tile.xIndex >= 0 && tile.yIndex >= 0 &&
                     tile.xIndex < maxTileIndex && tile.yIndex < maxTileIndex ) {
                     culledTiles.push( tile );
                }
            }
            this.tilesInView = culledTiles;
		},


		getTilesInView: function() {
            return this.tilesInView.slice( 0 ); // return copy
		},


		getTileBoundsInView: function() {
		    var bounds = this.getTileIterator().toTileBounds(),
		        maxTileIndex = Math.pow(2, this.getZoom() ) - 1;
            bounds.minX = Math.max( 0, bounds.minX );
            bounds.minY = Math.max( 0, bounds.minY );
            bounds.maxX = Math.min( maxTileIndex, bounds.maxX );
            bounds.maxY = Math.min( maxTileIndex , bounds.maxY );
			return bounds;
		},


        getMapWidth: function() {
            return this.getTileSize() * Math.pow( 2, this.getZoom() );
        },


        getMapHeight: function() {
            return this.getTileSize() * Math.pow( 2, this.getZoom() );
        },


		getViewportWidth: function() {
			return this.map.olMap_.viewPortDiv.clientWidth;
		},


		getViewportHeight: function() {
			return this.map.olMap_.viewPortDiv.clientHeight;
        },


		/**
		 * Returns the maps min and max pixels in viewport pixels
		 * NOTE:    viewport [0,0] is TOP-LEFT
		 *          map [0,0] is BOTTOM-LEFT
		 */
		getMapMinAndMaxInViewportPixels: function() {

		    var olMap = this.map.olMap_;

		    return {
                min : {
                    x: Math.round( olMap.minPx.x ),
                    y: Math.round( olMap.maxPx.y )
                },
                max : {
                    x: Math.round( olMap.maxPx.x ),
                    y: Math.round( olMap.minPx.y )
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
			    totalPixelSpan = this.getMapWidth(); // take into account any padding or margins

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
			var viewportMinMax = this.getMapMinAndMaxInViewportPixels();
			return {
				x: mx + viewportMinMax.min.x,
				y: this.getMapWidth() - my + viewportMinMax.max.y
			};
		},


		/**
		 * Transforms a point from data coordinates to map pixel coordinates
		 * NOTE:    data and map [0,0] are both BOTTOM-LEFT
		 */
		getMapPixelFromCoord: function(x, y) {
			var zoom = this.getZoom(),
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
        getTopLeftViewportPixelForTile: function( tilekey ) {

            var mapPixel = this.getTopLeftMapPixelForTile( tilekey );
            // transform map coord to viewport coord
            return this.getViewportPixelFromMapPixel( mapPixel.x, mapPixel.y );
        },


         /**
         * Returns the top left pixel location in viewport coord from a tile index
         */
        getTopLeftMapPixelForTile: function( tilekey ) {

            var parsedValues = tilekey.split(','),
                x = parseInt(parsedValues[1], 10),
                y = parseInt(parsedValues[2], 10),
                mx = x * TILESIZE,
                my = y * TILESIZE + TILESIZE;
            return {
                x : mx,
                y : my
            };
        },


        /**
         * Returns the data coordinate value corresponding to the top left pixel of the tile
         */
        getTopLeftCoordForTile: function( tilekey ) {
            var mapPixel = this.getTopLeftMapPixelForTile( tilekey );
            return this.getCoordFromMapPixel(mapPixel.x, mapPixel.y);
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

        /*
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
		*/

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

		setLayerIndex: function(layer, zIndex) {
			this.map.olMap_.setLayerIndex(layer, zIndex);
		},

		getLayerIndex: function(layer) {
			return this.map.olMap_.getLayerIndex(layer);
		},

		getExtent: function () {
			return this.map.olMap_.getExtent();
		},

		getZoom: function () {
			return this.map.olMap_.getZoom();
		},

		zoomToExtent: function (extent, findClosestZoomLvl) {
			this.map.olMap_.zoomToExtent(extent, findClosestZoomLvl);
		},

		updateSize: function() {
			this.map.olMap_.updateSize();
            this.redrawAxes();
		},


		getTileSize: function() {
			return TILESIZE;
		},


        panToCoord: function( x, y ) {
            var viewportPixel = this.getViewportPixelFromCoord( x, y ),
                lonlat = this.map.olMap_.getLonLatFromViewPortPx( viewportPixel );

            this.map.olMap_.panTo( lonlat );
        },


		on: function (eventType, callback) {

			var that = this;

			switch (eventType) {

			case 'click':
			case 'zoomend':
			case 'mousemove':
			case 'movestart':
			case 'moveend':
            case 'move':
				this.map.olMap_.events.register(eventType, this.map.olMap_, callback );
                break;

			case 'panend':

				/*
				 * ApertureJS 'panend' event is an alias to 'moveend' which also triggers
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
			case 'movestart':
			case 'moveend':
            case 'move':

				this.map.olMap_.events.unregister( eventType, this.map.olMap_, callback );
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
			case 'movestart':
			case 'moveend':
            case 'move':

				this.map.olMap_.events.triggerEvent(eventType, event);
				break;

			default:

				this.map.trigger(eventType, event);
				break;
			}
		}

	};

	return Map;
});
