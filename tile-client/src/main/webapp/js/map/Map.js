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

define( function( require ) {
	"use strict";

	var Layer = require('../layer/Layer'),
        BaseLayer = require('../layer/BaseLayer'),
        PubSub = require('../util/PubSub'),
	    AreaOfInterestTilePyramid = require('../binning/AreaOfInterestTilePyramid'),
	    WebMercatorTilePyramid = require('../binning/WebMercatorTilePyramid'),
	    TileIterator = require('../binning/TileIterator'),
	    TILESIZE = 256,
        setMapCallbacks;

    /**
     * Private: Set callbacks to update the maps tile focus, identifying which tile
     * the user is currently hovering over.
     *
     * @param map {Map} The map object.
     */
    setMapCallbacks = function( map ) {
        var previousMouse = {};
        function updateTileFocus( x, y ) {
            var tilekey = map.getTileKeyFromViewportPixel( x, y );
            if ( tilekey !== map.tileFocus ) {
                // only update tilefocus if it actually changes
                map.previousTileFocus = map.tileFocus;
                map.tileFocus = tilekey;
                PubSub.publish( 'layer', { field: 'tileFocus', value: tilekey });
            }
        }
        // set tile focus callbacks
        map.on('mousemove', function(event) {
            updateTileFocus( event.xy.x, event.xy.y );
            previousMouse.x = event.xy.x;
            previousMouse.y = event.xy.y;
        });
        map.on('zoomend', function(event) {
            updateTileFocus( previousMouse.x, previousMouse.y );
        });
        // if mousedown while map is panning, interrupt pan
        $( map.getElement() ).mousedown( function(){
            if ( map.olMap.panTween ) {
                 map.olMap.panTween.callbacks = null;
                 map.olMap.panTween.stop();
            }
        });
        // set resize callback
        $( window ).resize( function() {
            map.updateSize();
        });
    };

	function Map( id, spec ) {

        spec = spec || {};
        spec.options = spec.options || {};
        spec.pyramid = spec.pyramid || {};

        // element id
        this.id = id;
        // set map tile pyramid
        if ( spec.pyramid.type === "AreaOfInterest" ) {
            this.pyramid = new AreaOfInterestTilePyramid( spec.pyramid );
        } else {
            this.pyramid = new WebMercatorTilePyramid();
        }

        // create map object
        this.olMap = new OpenLayers.Map( this.id, {
            projection: new OpenLayers.Projection( spec.options.projection || "EPSG:900913" ),
            displayProjection: new OpenLayers.Projection( spec.options.displayProjection || "EPSG:4326" ),
            maxExtent: OpenLayers.Bounds.fromArray( spec.options.maxExtent || [
                -20037508.342789244,
				-20037508.342789244,
				20037508.342789244,
				20037508.342789244
            ]),
            units: spec.options.units || "m",
            numZoomLevels: spec.options.numZoomLevels || 18,
            controls: [
                new OpenLayers.Control.Navigation({ documentDrag: true }),
                new OpenLayers.Control.Zoom()
            ]
        });

        // initialize base layer index to -1 for no baselayer
        this.baseLayerIndex = -1;
    }

    Map.prototype = {

        addAxis: function ( axis ) {

            // set min/max based on pyramid
            if ( axis.position === 'top' || axis.position === 'bottom' ) {
                axis.min = this.pyramid.minX;
                axis.max = this.pyramid.maxX;
            } else {
                axis.min = this.pyramid.minY;
                axis.max = this.pyramid.maxY;
            }

            // activate and attach to map
            axis.map = this;
            axis.activate();
            this.axes = this.axes || {};
            this.axes[ axis.position ] = axis;

            // update dimensions
            _.forIn( this.axes, function( value ) {
                value.setContentDimension();
            });

            // redraw
            _.forIn( this.axes, function( value ) {
                value.redraw();
            });
        },

        /**
         * Adds a Layer object to the map.
         *
         * @param layer {Layer} The layer object.
         */
        add: function( layer ) {
            // set the map attribute on the layer to this map
            layer.map = this;
            if ( layer instanceof BaseLayer ) {
                // add to baselayer array
                this.baselayers = this.baselayers || [];
                this.baselayers.push( layer );
                // if first baselayer, activate the map
                if ( this.baseLayerIndex < 0 ) {
                    // openlayers maps require a baselayer to operate, once
                    // this baselayer is set, activate the map
                    this.setBaseLayerIndex( 0 );
                    // set initial viewpoint, required by openlayers
                    this.olMap.zoomToMaxExtent();
                    // set mouse callbacks
                    setMapCallbacks( this );
                }
            } else if ( layer instanceof Layer ) {
                // activate the layer
                layer.activate();
                // add it to layer map
                this.layers = this.layers || {};
                this.layers[ layer.getUUID() ] = layer;
            }
        },

        /**
         * Removes a Layer object from the map.
         *
         * @param layer {Layer} The layer object.
         */
        remove: function( layer ) {
            var index;
            if ( layer instanceof BaseLayer ) {
                // if only 1 baselayer available, ignore
                if ( this.baselayers.length === 1 ) {
                    console.error( 'Error: attempting to remove only baselayer from map, this destroys the map, use destroy() instead' );
                    return;
                }
                // get index of baselayer
                index = this.baselayers.indexOf( layer );
                // remove baselayer from array
                this.baselayers.splice( index, 1 );
                // get replacement index
                index = ( this.baselayers[ index ] ) ? index : index-1;
                // replace baselayer
                this.setBaseLayerIndex( index );
            } else {
                // deactivate the layer
                layer.deactivate();
                // remove it from layer map
                delete this.layers[ layer.getUUID() ];
            }
            // set the map attribute on the layer to this map
            layer.olMap = null;
        },

        /**
         * Returns the tilekey for the tile currently under the mouse.
         */
        getTileFocus: function() {
            return this.tileFocus;
        },

        /**
         * If multiple baselayers are attached to the map, this function is
         * used to change the index.
         *
         * @param index {int} The index of the baselayer to switch to.
         */
        setBaseLayerIndex: function( index ) {
            var oldBaseLayer = this.baselayers[ this.baseLayerIndex ],
                newBaseLayer = this.baselayers[ index ];
            if ( !newBaseLayer ) {
                console.error("Error, no baselayer for supplied index: " + index );
                return;
            }
            if ( oldBaseLayer ) {
                oldBaseLayer.deactivate();
            }
            newBaseLayer.activate();
            this.baseLayerIndex = index;
            PubSub.publish( newBaseLayer.getChannel(), { field: 'baseLayerIndex', value: index });
        },

        /**
         * Returns the currently active baselayer index.
         *
         * @returns {number|*}
         */
        getBaseLayerIndex: function() {
            return this.baseLayerIndex;
        },

        /**
         * Returns the current theme of the map. Currently restricted to "dark"
         * and "light".
         *
         * @returns {string} The theme of the map.
         */
        getTheme: function() {
        	return $( this.olMap.div ).hasClass( "light-theme" ) ? 'light' : 'dark';
        },

        /**
         * Returns the map DOM element. This is the element to which
         * the map object is 'attached'.
         *
         * @returns {HTMLElement} The map div element.
         */
        getElement:  function() {
            return this.olMap.div;
        },

        /**
         * Returns the map viewport DOM element. This the element that matches
         * the viewable portion of the map.
         *
         * @returns {HTMLElement} The map viewport div element.
         */
        getViewportElement:  function() {
            return this.olMap.viewPortDiv;
        },

        /**
         * Returns the map container DOM element. This is the element to which all
         * 'pannable' layers are attached to.
         *
         * @returns {HTMLElement} The map container div element.
         */
        getContainerElement:  function() {
            return this.olMap.layerContainerDiv;
        },

        /**
         * Returns the tile pyramid used for the map.
         *
         * @returns { AreaOfInterestTilePyramid | WebMercatorTilePyramid } TilePyramid Object.
         */
		getPyramid: function() {
			return this.pyramid;
		},


        /**
         * Returns a TileIterator object. This TileIterator contains all viewable
         * tiles currently in the map.
         *
         * @returns {TileIterator} TileIterator object.
         */
		getTileIterator: function() {
			var level = this.olMap.getZoom(),
			    // Current map bounds, in meters
			    bounds = this.olMap.getExtent(),
			    // Total map bounds, in meters
			    extents = this.olMap.getMaxExtent(),
			    // Pyramid for the total map bounds
			    pyramid = new AreaOfInterestTilePyramid({
                    minX: extents.left,
                    minY: extents.bottom,
                    maxX: extents.right,
                    maxY: extents.top
                });
			// determine all tiles in view
			return new TileIterator({
                pyramid: pyramid,
                level: level,
                minX: bounds.left,
                minY: bounds.bottom,
                maxX: bounds.right,
                maxY: bounds.top
            });
		},

        /**
         * Returns an array of all tilekeys currently in view.
         *
         * @returns {Array} An array of tilekey strings.
         */
		getTilesInView: function() {
            var tiles = this.getTileIterator().getRest(),
                culledTiles = [],
                maxTileIndex = Math.pow(2, this.getZoom() ),
                tile,
                i;
            for (i=0; i<tiles.length; i++) {
                tile = tiles[i];
                if ( tile.xIndex >= 0 && tile.yIndex >= 0 &&
                     tile.xIndex < maxTileIndex && tile.yIndex < maxTileIndex ) {
                     culledTiles.push( tile.level + "," + tile.xIndex + "," + tile.yIndex );
                }
            }
            return culledTiles;
		},

        /**
         * Zooms the map to a particular coordinate, and zoom level. This
         * transition is instantaneous.
         *
         * @param x    {float} x coordinate (longitude for geospatial)
         * @param y    {float} y coordinate (latitude for geospatial)
         * @param zoom {int}   zoom level
         */
        zoomTo: function( x, y, zoom ) {
            var projection,
                viewportPx,
                lonlat;
            if ( this.pyramid instanceof WebMercatorTilePyramid ) {
                // geo-spatial map
                projection = new OpenLayers.Projection('EPSG:4326');
                lonlat = new OpenLayers.LonLat( x, y );
                if( this.olMap.getProjection() !== projection.projCode ) {
                    lonlat.transform( projection, this.olMap.projection );
                }
                this.olMap.setCenter( lonlat, zoom );
            } else {
                // linear bi-variate map
                viewportPx = this.getViewportPixelFromCoord( x, y );
                lonlat = this.olMap.getLonLatFromViewPortPx( viewportPx );
                this.olMap.setCenter( lonlat, zoom );
            }
        },

        /**
         * Pans the map to a particular coordniate. This
         * transition is gradual.
         *
         * @param x    {float} x coordinate (longitude for geospatial)
         * @param y    {float} y coordinate (latitude for geospatial)
         */
        panTo: function( x, y ) {
            var projection,
                viewportPx,
                lonlat;
            if ( this.pyramid instanceof WebMercatorTilePyramid ) {
                // geo-spatial map
                projection = new OpenLayers.Projection('EPSG:4326');
                lonlat = new OpenLayers.LonLat( x, y );
                if( this.olMap.getProjection() !== projection.projCode ) {
                    lonlat.transform( projection, this.olMap.projection );
                }
                this.olMap.panTo( lonlat );
            } else {
                // linear bi-variate map
                viewportPx = this.getViewportPixelFromCoord( x, y );
                lonlat = this.olMap.getLonLatFromViewPortPx( viewportPx );
                this.olMap.panTo( lonlat );
            }
        },

        getMapWidth: function() {
            return this.getTileSize() * Math.pow( 2, this.getZoom() );
        },

        getMapHeight: function() {
            return this.getTileSize() * Math.pow( 2, this.getZoom() );
        },

		getViewportWidth: function() {
			return this.olMap.viewPortDiv.clientWidth;
		},

		getViewportHeight: function() {
			return this.olMap.viewPortDiv.clientHeight;
        },

		/**
		 * Returns the maps min and max pixels in viewport pixels
         *
		 * NOTE:    viewport [0,0] is TOP-LEFT
		 *          map [0,0] is BOTTOM-LEFT
		 */
		getMapMinAndMaxInViewportPixels: function() {
		    var map = this.olMap;
		    return {
                min : {
                    x: Math.round( map.minPx.x ),
                    y: Math.round( map.maxPx.y )
                },
                max : {
                    x: Math.round( map.maxPx.x ),
                    y: Math.round( map.minPx.y )
                }
            };
		},

		/**
		 * Transforms a point from viewport pixel coordinates to map pixel coordinates
         *
		 * NOTE:    viewport [0,0] is TOP-LEFT
		 *          map [0,0] is BOTTOM-LEFT
		 */
		getMapPixelFromViewportPixel: function(vx, vy) {
			var viewportMinMax = this.getMapMinAndMaxInViewportPixels(),
			    totalPixelSpan = this.getMapWidth();
			return {
				x: totalPixelSpan + vx - viewportMinMax.max.x,
				y: totalPixelSpan - vy + viewportMinMax.max.y
			};
		},

		/**
		 * Transforms a point from map pixel coordinates to viewport pixel coordinates
         *
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
         *
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
         *
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
         *
		 * NOTE:    viewport [0,0] is TOP-LEFT
		 *          data [0,0] is BOTTOM-LEFT
		 */
		getCoordFromViewportPixel: function(vx, vy) {
			var mapPixel = this.getMapPixelFromViewportPixel(vx, vy);
			return this.getCoordFromMapPixel(mapPixel.x, mapPixel.y);
		},

		/**
		 * Transforms a point from data coordinates to viewport pixel coordinates
         *
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

		addControl: function(control) {
			return this.olMap.addControl(control);
		},

		setLayerIndex: function(layer, zIndex) {
			this.olMap.setLayerIndex(layer, zIndex);
		},

		getExtent: function () {
			return this.olMap.getExtent();
		},

		getZoom: function () {
			return this.olMap.getZoom();
		},

		zoomToExtent: function (extent, findClosestZoomLvl) {
			this.olMap.zoomToExtent(extent, findClosestZoomLvl);
		},

		updateSize: function() {
			this.olMap.updateSize();
		},

		getTileSize: function() {
			return TILESIZE;
		},

		on: function (eventType, callback) {
            this.olMap.events.register(eventType, this.olMap, callback);
		},

		off: function(eventType, callback) {
			this.olMap.events.unregister( eventType, this.olMap, callback );
		},

		trigger: function(eventType, event) {
            this.olMap.events.triggerEvent( eventType, event );
		}

	};

	return Map;
});
