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

	var Axis = require('./Axis'),
        Layer = require('../layer/Layer'),
        BaseLayer = require('../layer/BaseLayer'),
        PubSub = require('../util/PubSub'),
        Util = require('../util/Util'),
	    AreaOfInterestTilePyramid = require('../binning/AreaOfInterestTilePyramid'),
	    WebMercatorTilePyramid = require('../binning/WebMercatorTilePyramid'),
	    TileIterator = require('../binning/TileIterator'),
	    TILESIZE = 256,
        setMapCallbacks,
        activateComponent,
        deactivateComponent,
        activateDeferredComponents,
        addBaseLayer,
        addLayer,
        addAxis,
        removeBaseLayer,
        removeLayer,
        removeAxis;

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
            map.olMap.updateSize();
        });
    };

    /**
     * Private: Activates a component.
     *
     * @param map       {Map} The map object.
     * @param component {*}   The component to activate.
     */
    activateComponent = function( map, component ) {
        if ( component instanceof Layer ) {
            addLayer( map, component );
        } else if ( component instanceof Axis ) {
            addAxis( map, component );
        }
    };

    /**
     * Private: Deactivates a component.
     *
     * @param map       {Map} The map object.
     * @param component {*}   The component to deactivate.
     */
    deactivateComponent = function( map, component ) {
        if ( component instanceof BaseLayer ) {
            removeBaseLayer( map, component );
        } else if ( component instanceof Layer ) {
            removeLayer( map, component );
        } else if ( component instanceof Axis ) {
            removeAxis( map, component );
        }
    };

    /**
     * Private: Activates deferred components when the map is ready.
     *
     * @param map {Map} The map object.
     */
    activateDeferredComponents = function( map ) {
        var i;
        for ( i=0; i<map.deferreds.length; i++ ) {
            activateComponent( map, map.deferreds[i] );
        }
        delete map.deferreds;
    };

    /**
     * Private: Adds a base layer to the map. If no baselayer is attached, it
     * will also activate it, along with any deferred components that were attached
     * first.
     *
     * @param map       {Map}       The map object.
     * @param baselayer {BaseLayer} The baselayer object.
     */
    addBaseLayer = function( map, baselayer ) {
        // add map to baselayer
        baselayer.map = map;
        // add to baselayer array
        map.baselayers = map.baselayers || [];
        map.baselayers.push( baselayer );
        // if first baselayer, activate the map
        if ( map.baseLayerIndex < 0 ) {
            // openlayers maps require a baselayer to operate, once
            // this baselayer is set, activate the map
            map.setBaseLayerIndex( 0 );
            // set initial viewpoint, required by openlayers
            map.olMap.zoomToMaxExtent();
            // set mouse callbacks
            setMapCallbacks( map );
            if ( map.deferreds ) {
                activateDeferredComponents( map );
            }
        }
    };

    /**
     * Private: Adds a layer object to the map and activates it.
     *
     * @param map       {Map}   The map object.
     * @param layer {Layer} The layer object.
     */
    addLayer = function( map, layer ) {
        // add map to layer
        layer.map = map;
        // activate the layer
        layer.activate();
        // add it to layer map
        map.layers = map.layers || {};
        map.layers[ layer.getUUID() ] = layer;
    };

    /**
     * Private: Adds an Axis object to the map and activates it.
     *
     * @param map  {Map}   The map object.
     * @param axis {Axis} The layer object.
     */
    addAxis = function( map, axis ) {
        // set min/max based on pyramid
        if ( axis.position === 'top' || axis.position === 'bottom' ) {
            axis.min = map.pyramid.minX;
            axis.max = map.pyramid.maxX;
        } else {
            axis.min = map.pyramid.minY;
            axis.max = map.pyramid.maxY;
        }

        // activate and attach to map
        axis.map = map;
        axis.activate();
        map.axes = map.axes || {};
        map.axes[ axis.position ] = axis;

        // update dimensions
        _.forIn( map.axes, function( value ) {
            value.setContentDimension();
        });

        // redraw
        _.forIn( map.axes, function( value ) {
            value.redraw();
        });
    };

    /**
     * Private: Removes a base layer from the map. If no other baselayer is attached, it
     * will refuse to do so.
     *
     * @param map       {Map}       The map object.
     * @param baselayer {BaseLayer} The baselayer object.
     */
    removeBaseLayer = function( map, baselayer ) {
        var index;
        // if only 1 baselayer available, ignore
        if ( map.baselayers.length === 1 ) {
            console.error( 'Error: attempting to remove only baselayer from map, this destroys the map, use destroy() instead' );
            return;
        }
        // get index of baselayer
        index = map.baselayers.indexOf( baselayer );
        // remove baselayer from array
        map.baselayers.splice( index, 1 );
        // get replacement index
        index = ( map.baselayers[ index ] ) ? index : index-1;
        // replace baselayer
        map.setBaseLayerIndex( index );
        baselayer.map = null;
    };

    /**
     * Private: Removes a layer object from the map and deactivates it.
     *
     * @param map       {Map}   The map object.
     * @param layer {Layer} The layer object.
     */
    removeLayer = function( map, layer ) {
        // remove it from layer map
        delete map.layers[ layer.getUUID() ];
        // deactivate it
        layer.deactivate();
        layer.map = null;
    };

    /**
     * Private: Removes an Axis object from the map and deactivates it.
     *
     * @param map  {Map}   The map object.
     * @param axis {Axis} The layer object.
     */
    removeAxis = function( map, axis ) {
        // remove it from axes map
        delete map.axes[ axis.position ];
        // deactivate it
        axis.deactivate();
        axis.map = null;
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

        /**
         * Adds a component to the map.
         *
         * @param component {*} The component object.
         */
        add: function( component ) {
            if ( component instanceof BaseLayer ) {
                // if a baselayer, add it
                addBaseLayer( this, component );
                return;
            }
            if ( this.baseLayerIndex < 0 ) {
                // if no baselayer is attached yet, we cannot activate the layers
                // add them to list of deferred activations
                this.deferreds = this.deferreds || [];
                this.deferreds.push( component );
                return;
            }
            // activate the component
            activateComponent( this, component );
        },

        /**
         * Removes a component from the map.
         *
         * @param component {*} The component object.
         */
        remove: function( component ) {
            // activate the component
            deactivateComponent( this, component );
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
            return TILESIZE * Math.pow( 2, this.getZoom() );
        },

        getMapHeight: function() {
            return TILESIZE * Math.pow( 2, this.getZoom() );
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
		getMapPixelFromViewportPixel: function( vx, vy ) {
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
		getCoordFromMapPixel: function( mx, my ) {
			var tileAndBin = this.getTileAndBinFromMapPixel( mx, my, TILESIZE, TILESIZE ),
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
		getCoordFromViewportPixel: function( vx, vy ) {
			var mapPixel = this.getMapPixelFromViewportPixel( vx, vy );
			return this.getCoordFromMapPixel( mapPixel.x, mapPixel.y );
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
        getTileAndBinFromMapPixel: function( mx, my, xBinCount, yBinCount ) {

            var tileIndexX = Math.floor( mx / TILESIZE ),
                tileIndexY = Math.floor( my / TILESIZE ),
                tilePixelX = Util.mod( mx , TILESIZE ),
                tilePixelY = Util.mod( my, TILESIZE );
            return {
                tile: {
                    level : this.getZoom(),
                    xIndex : tileIndexX,
                    yIndex : tileIndexY,
                    xBinCount : xBinCount,
                    yBinCount : yBinCount
                },
                bin: {
                    x : Math.floor( tilePixelX / (TILESIZE / xBinCount ) ),
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

		getZoom: function () {
			return this.olMap.getZoom();
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
