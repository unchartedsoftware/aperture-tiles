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

            var that = this;

            this.id = id;
            this.$map = $( "#" + this.id );
            this.axes = [];
            this.pyramid = PyramidFactory.createPyramid( spec.PyramidConfig );
            this.baseLayers = ( $.isArray( spec.MapConfig.baseLayer ) ) ? spec.MapConfig.baseLayer : [spec.MapConfig.baseLayer];

            if ( this.baseLayers.length === 0 ) {
                this.baseLayers[0] = {
                    "type" : "BlankBase",
                    "options" : {
                        "name" : "black",
                        "color" : "rgb(0,0,0)"
                    }
                };
            }

            // Set the map configuration
            spec.MapConfig.baseLayer = {}; // default to no base layer
			aperture.config.provide({
				'aperture.map' : {
					'defaultMapConfig' : spec.MapConfig
				}
			});


			// Initialize the map
			this.map = new aperture.geo.Map({ 
				id: this.id,
                options: {
                    controls: [
                        new OpenLayers.Control.Navigation({ documentDrag: true }),
                        new OpenLayers.Control.Zoom()
                    ]
                }
			});

            // set proper base layer
			this.setBaseLayerIndex( 0 );

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

            // set resize callback
            $(window).resize( $.proxy(this.updateSize, this) );

			// Trigger the initial resize event to resize everything
			$(window).resize();

			if(spec.MapConfig.zoomTo) {
                this.map.zoomTo( spec.MapConfig.zoomTo[0],
                                 spec.MapConfig.zoomTo[1],
                                 spec.MapConfig.zoomTo[2] );
            }
		},


        createRoot: function() {

            var that = this;
            this.$root = $('<div id="'+this.id+'-root" style="position:absolute; top:0px; z-index:999;"></div>');
            this.$map.append( this.$root );

            this.on('move', function() {
                var pos = that.getViewportPixelFromMapPixel( 0, that.getMapHeight() );
                that.$root.css({ "-webkit-transform":"translate("+ pos.x +"px, " + pos.y + "px)"});
            });

            this.trigger('move'); // fire initial move event
        },


        setBaseLayerIndex: function(index) {

            var $map = this.getElement(),
                olMap_ = this.map.olMap_,
                newBaseLayerConfig = this.baseLayers[index],
                newBaseLayerType,
                newBaseLayer;

            if( newBaseLayerConfig.type === 'BlankBase' ) {

                // changing to blank base layer
                $map.css( 'background-color', newBaseLayerConfig.options.color );
                olMap_.baseLayer.setVisibility(false);

            } else {

                // destroy previous baselayer
                olMap_.baseLayer.destroy();
                //reset the background color to black
                $map.css( 'background-color', 'rgb(0,0,0)' );
                // create new layer instsance
                newBaseLayerType = (newBaseLayerConfig.type === 'Google') ? aperture.geo.MapTileLayer.Google : aperture.geo.MapTileLayer.TMS;
                newBaseLayer = this.map.addLayer( newBaseLayerType, {}, newBaseLayerConfig );
                // attach, and refresh it by toggling visibility
                olMap_.baseLayer = newBaseLayer.olLayer_;
                olMap_.setBaseLayer( newBaseLayer.olLayer_ );
                olMap_.baseLayer.setVisibility(false);
                olMap_.baseLayer.setVisibility(true);
            }
        },


        /**
         *
         * @param mapConfig
         * @param plotDiv optional div container id of the plot - useful when multiple maps are present
         */
        setTileBorderStyle: function ( mapConfig ) {

            var olTileImageConfig = mapConfig.TileBorderConfig;

            //if it is not defined, don't set border style
            if( !olTileImageConfig ){
                return;
            }

            if( olTileImageConfig === 'default' ){
                olTileImageConfig = {
                    "color" : "rgba(255, 255, 255, .5)",
                    "style" : "solid",
                    "weight" : "1px"
                };
            }

            //set individual defaults if they are omitted.
            olTileImageConfig.color = olTileImageConfig.color || "rgba(255, 255, 255, .5)";
            olTileImageConfig.style = olTileImageConfig.style || "solid";
            olTileImageConfig.weight = olTileImageConfig.weight || "1px";

            $(document.body).prepend(
                $('<style type="text/css">' + ('#' + this.id) + ' .olTileImage {' +
                    'border-left : ' + olTileImageConfig.weight + ' ' + olTileImageConfig.style + ' ' + olTileImageConfig.color +
                    '; border-top : ' + olTileImageConfig.weight + ' ' + olTileImageConfig.style + ' ' + olTileImageConfig.color +';}' +
                  '</style>')
            );
        },


        getZIndex: function() {
            var indices = OpenLayers.Map.prototype.Z_INDEX_BASE,
                maxZ = 0,
                key;
            for (key in indices) {
                if (indices.hasOwnProperty(key)) {
                    maxZ = Math.max( maxZ, indices[key] );
                }
            }
            return maxZ;
        },


        getElement:  function() {
            return this.$map;
        },


        getRootElement: function() {
            return this.$root;
        },


        getEventHandlingDOMElement: function() {
            return $('.olMapViewport')[0];
        },


        /**
         * Allows the given DOMElement or jQuery object events to propagate through
         * and interact with the underlying Map
         */
        enableEventToMapPropagation: function( elem, events ) {

            var //that = this,
                domElement = (elem instanceof jQuery) ? elem[0] : elem,
                i;

            function propagateEvent( event ) {
                var newEvent = new event.constructor(event.type, event),
                    below;
                $(elem).css('pointer-events', 'none');
                below = document.elementFromPoint(event.clientX, event.clientY); //that.getEventHandlingDOMElement();
                if (below) {
                    below.dispatchEvent(newEvent);
                }
                $(elem).css('pointer-events', 'all');
            }

            if (!events) {
                domElement.onmousedown = propagateEvent;
                domElement.onmouseup = propagateEvent;
                domElement.onmousemove = propagateEvent;
                domElement.onwheel = propagateEvent;
                domElement.onmousewheel = propagateEvent;
                domElement.onscroll = propagateEvent;
                domElement.onclick = propagateEvent;
                domElement.ondblclick = propagateEvent;
            } else {
                events = ($.isArray) ? events : [events];
                for (i=0; i<events.length; i++) {
                    domElement[events[i]] = propagateEvent;
                }
            }

        },


        disableEventToMapPropagation: function( elem, events ) {

            var domElement = (elem instanceof jQuery) ? elem[0] : elem,
                i;
            if (!events) {
                domElement.onmousedown = null;
                domElement.onmouseup = null;
                domElement.onmousemove = null;
                domElement.onwheel = null;
                domElement.onmousewheel = null;
                domElement.onscroll = null;
                domElement.onclick = null;
                domElement.ondblclick = null;
            } else {
                events = ($.isArray) ? events : [events];
                for (i=0; i<events.length; i++) {
                    domElement[events[i]] = null;
                }
            }
        },

		setAxisSpecs: function (axes) {

			var i, spec;
			for (i=0; i< axes.length; i++) {
				spec = axes[i];
				spec.mapId = this.id;
				spec.map = this;
				this.axes.push( new Axis(spec) );
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
                     culledTiles.push( tile );
                }
            }
			return culledTiles;
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

        getMapkey : function() {
            var minMax = this.getMapMinAndMaxInViewportPixels();
            return minMax.min.x + ":"
                 + minMax.max.x + ","
                 + minMax.min.y + ":"
                 + minMax.max.y;
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

		setOpacity: function( opacity ) {
		    this.map.olMap_.baseLayer.setOpacity ( opacity );
		},

		setVisibility: function( visibility ) {
            this.map.olMap_.baseLayer.setVisibility( visibility );
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
            this.map.panTo( y, x );
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
