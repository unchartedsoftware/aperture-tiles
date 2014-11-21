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

define(function (require) {
	"use strict";

	var BaseLayer = require('../layer/BaseLayer'),
        ServerLayer = require('../layer/ServerLayer'),
        ClientLayer = require('../layer/ClientLayer'),
        PubSub = require('../util/PubSub'),
	    AreaOfInterestTilePyramid = require('../binning/AreaOfInterestTilePyramid'),
	    WebMercatorTilePyramid = require('../binning/WebMercatorTilePyramid'),
	    TileIterator = require('../binning/TileIterator'),
	    Axis =  require('./Axis'),
	    TILESIZE = 256,
        tileEventSubscribers = {},
        setMapCallbacks;

    /**
     * Set callbacks to update the maps tile focus, identifying which tile the user is currently
     * hovering over.
     */
    setMapCallbacks = function( map ) {
        var previousMouse = {};
        function updateTileFocus( layer, x, y ) {
            var tilekey = map.getTileKeyFromViewportPixel( x, y );
            if ( tilekey !== map.getTileFocus() ) {
                // only update tilefocus if it actually changes
                map.previousTileFocus = map.getTileFocus();
                map.tileFocus = tilekey;
                PubSub.publish( 'layer', { field: 'tileFocus', value: tilekey });
            }
        }
        // set tile focus callbacks
        map.on('mousemove', function(event) {
            updateTileFocus( map, event.xy.x, event.xy.y );
            previousMouse.x = event.xy.x;
            previousMouse.y = event.xy.y;
        });
        map.on('zoomend', function(event) {
            updateTileFocus( map, previousMouse.x, previousMouse.y );
        });
        // if mousedown while map is panning, interrupt pan
        map.getElement().mousedown( function(){
            if ( map.map.panTween ) {
                 map.map.panTween.callbacks = null;
                 map.map.panTween.stop();
            }
        });
        // set resize callback
        $( window ).resize( function() {
            map.updateSize();
        });
    };

	function Map( spec ) {

        var options = spec.options;

        this.id = spec.id;
        this.$map = $( "#" + this.id );

        // set map tile pyramid
        if ( "AreaOfInterest" === spec.pyramid.type ) {
            this.pyramid = new AreaOfInterestTilePyramid( spec.pyramid );
        } else if ( "WebMercator" === spec.pyramid.type ) {
            this.pyramid = new WebMercatorTilePyramid();
        }

        // set the map configuration
        this.map = new OpenLayers.Map( this.id, {
            projection: new OpenLayers.Projection( options.projection ),
            displayProjection: new OpenLayers.Projection( options.displayProjection ),
            maxExtent: OpenLayers.Bounds.fromArray( options.maxExtent ),
            units: options.units || "m",
            numZoomLevels: options.numZoomLevels || 18,
            controls: [
                new OpenLayers.Control.Navigation({ documentDrag: true }),
                new OpenLayers.Control.Zoom()
            ]
        });

        this.baseLayerIndex = -1;
    }

    Map.prototype = {

        activate: function() {
            // set initial viewpoint, required by openlayers
            this.map.zoomToMaxExtent();
            // create div root layer
            this.createRoot();
            // set mouse callbacks
            setMapCallbacks( this );
        },

        deactivate: function() {
            // TODO:
            return true;
        },

        zoomTo: function( lat, lon, zoom ) {
            var projection = new OpenLayers.Projection('EPSG:4326'),
                center = new OpenLayers.LonLat( lon, lat );
			if( this.map.getProjection() !== projection.projCode ) {
				center.transform( projection, this.map.projection );
			}
			this.map.setCenter( center, zoom );
        },

        add: function( layer ) {
            if ( layer instanceof BaseLayer ) {
                // baselayer
                this.baselayers = this.baselayers || [];
                layer.map = this;
                this.baselayers.push( layer );
                if ( this.baseLayerIndex < 0 ) {
                    this.setBaseLayerIndex( 0 );
                    this.activate();
                }
            } else if ( layer instanceof ServerLayer ) {
                // server layer
                this.serverLayers = this.serverLayers || [];
                layer.map = this;
                layer.activate();
                this.serverLayers.push( layer );
            } else if ( layer instanceof ClientLayer ) {
                // client layer
                this.clientLayers = this.clientLayers || [];
                layer.map = this;
                layer.activate();
                this.clientLayers.push( layer );
            }
        },

        /**
         * Get the layers tile focus.
         */
        getTileFocus: function() {
            return this.tileFocus;
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
            var oldBaseLayer = this.baselayers[ this.baseLayerIndex ],
                newBaseLayer = this.baselayers[ index ];
            if ( oldBaseLayer ) {
                oldBaseLayer.deactivate();
            }
            newBaseLayer.activate();
            this.baseLayerIndex = index;
            PubSub.publish( newBaseLayer.getChannel(), { field: 'baseLayerIndex', value: index });
        },

        getTheme: function() {
        	return $("body").hasClass("light-theme")? 'light' : 'dark';
        },

        getBaseLayerIndex: function() {
            return this.baseLayerIndex;
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

		getAxes: function() {
			return this.axes;
		},

		getPyramid: function() {
			return this.pyramid;
		},

		getTileIterator: function() {
			var level = this.map.getZoom(),
			    // Current map bounds, in meters
			    bounds = this.map.getExtent(),
			    // Total map bounds, in meters
			    extents = this.map.getMaxExtent(),
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


        updateTilesInView: function() {
            var tiles = this.getTileIterator().getRest(),
                culledTiles = [],
                maxTileIndex = Math.pow(2, this.getZoom() ),
                tile, removed = [], added = [],
                i;
            for (i=0; i<tiles.length; i++) {
                tile = tiles[i];
                if ( tile.xIndex >= 0 && tile.yIndex >= 0 &&
                     tile.xIndex < maxTileIndex && tile.yIndex < maxTileIndex ) {
                     culledTiles.push( tile.level + "," + tile.xIndex + "," + tile.yIndex );
                }
            }

            this.previousTilesInView = this.tilesInView;
            this.tilesInView = culledTiles;

            if ( this.previousTilesInView ) {
                // determine which tiles have been added and removed from view
                for ( i=0; i<this.previousTilesInView.length; i++ ) {
                    tile = this.previousTilesInView[i];
                    if ( this.tilesInView.indexOf( tile ) === -1 ) {
                        removed.push( tile );
                    }
                }
                for ( i=0; i<this.tilesInView.length; i++ ) {
                    tile = this.tilesInView[i];
                    if ( this.previousTilesInView.indexOf( tile ) === -1 ) {
                        added.push( tile );
                    }
                }
                // if a tile has been added or removed, trigger 'tile' event
                if ( added.length > 0 || removed.length > 0 ) {
                    this.trigger( 'tile', { added: added, removed: removed } );
                }
            } else {
                this.trigger( 'tile', { added: this.tilesInView.slice(0), removed: [] } );
            }
		},

		getTilesInView: function() {
            return this.tilesInView.slice( 0 ); // return copy
		},

        getMapWidth: function() {
            return this.getTileSize() * Math.pow( 2, this.getZoom() );
        },

        getMapHeight: function() {
            return this.getTileSize() * Math.pow( 2, this.getZoom() );
        },

		getViewportWidth: function() {
			return this.map.viewPortDiv.clientWidth;
		},

		getViewportHeight: function() {
			return this.map.viewPortDiv.clientHeight;
        },

		/**
		 * Returns the maps min and max pixels in viewport pixels
		 * NOTE:    viewport [0,0] is TOP-LEFT
		 *          map [0,0] is BOTTOM-LEFT
		 */
		getMapMinAndMaxInViewportPixels: function() {

		    var map = this.map;

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

		addLayer: function(layer) {
			return this.map.addLayer(layer);
		},

        removeLayer: function(layer) {
			return this.map.addLayer(layer);
		},

		addControl: function(control) {
			return this.map.addControl(control);
		},

		setLayerIndex: function(layer, zIndex) {
			this.map.setLayerIndex(layer, zIndex);
		},

		getExtent: function () {
			return this.map.getExtent();
		},

		getZoom: function () {
			return this.map.getZoom();
		},

		zoomToExtent: function (extent, findClosestZoomLvl) {
			this.map.zoomToExtent(extent, findClosestZoomLvl);
		},

		updateSize: function() {
			this.map.updateSize();
		},

		getTileSize: function() {
			return TILESIZE;
		},

        panToCoord: function( x, y ) {
            var viewportPixel = this.getViewportPixelFromCoord( x, y ),
                lonlat = this.map.getLonLatFromViewPortPx( viewportPixel );

            this.map.panTo( lonlat );
        },

		on: function (eventType, callback) {

            switch (eventType) {

                case 'tile':

                    tileEventSubscribers[ this.id ] = tileEventSubscribers[ this.id ] || [];
                    tileEventSubscribers[ this.id ].push(callback);
                    break;

                default:

                    this.map.events.register(eventType, this.map, callback);
                    break;
            }
		},

		off: function(eventType, callback) {

			switch (eventType) {

                case 'tile':

                    if ( tileEventSubscribers[ this.id ] ) {
                        tileEventSubscribers[ this.id ].remove( tileEventSubscribers[ this.id ].splice( tileEventSubscribers[ this.id ].indexOf( callback ) ) );
                    }
                    break;

                default:

                    this.map.events.unregister( eventType, this.map, callback );
                    break;
			}
		},

		trigger: function(eventType, event) {

            var i;

			switch (eventType) {

                case 'tile':

                    if ( tileEventSubscribers[ this.id ] ) {
                        for ( i=0; i<tileEventSubscribers[ this.id ].length; i++ ) {
                            tileEventSubscribers[ this.id ][i]( event );
                        }
                    }
                    break;

                default:

                    this.map.events.triggerEvent( eventType, event );
                    break;
			}
		}

	};

	return Map;
});
