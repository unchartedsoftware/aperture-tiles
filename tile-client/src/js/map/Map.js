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

    var Axis = require('./Axis'),
        PendingLayer = require('../layer/PendingLayer'),
        MapUtil = require('./MapUtil'),
        Layer = require('../layer/Layer'),
        Carousel = require('../layer/Carousel'),
        BaseLayer = require('../layer/BaseLayer'),
        PubSub = require('../util/PubSub'),
        AreaOfInterestTilePyramid = require('../binning/AreaOfInterestTilePyramid'),
        WebMercatorTilePyramid = require('../binning/WebMercatorTilePyramid'),
        TileIterator = require('../binning/TileIterator'),
        TILESIZE = 256,
        MARKER_Z_INDEX = 5000,
        setMapCallbacks,
        activateComponent,
        deactivateComponent,
        activateDeferredComponents,
        addBaseLayer,
        addLayer,
        addCarousel,
        addAxis,
        removeBaseLayer,
        removeLayer,
        removeCarousel,
        removeAxis,
        resetLayerZIndices;

    /**
     * Set callbacks to update the maps tile focus, identifying which tile
     * the user is currently hovering over.
     * @private
     *
     * @param map {Map} The map object.
     */
    setMapCallbacks = function( map ) {
        var previousMouse = {};
        function updateTileFocus( x, y ) {
            var tileAndBin = MapUtil.getTileAndBinFromViewportPixel( map, x, y, 1, 1 ),
                tilekey = tileAndBin.tile.level + ","
                    + tileAndBin.tile.xIndex + ","
                    + tileAndBin.tile.yIndex;
            if ( tilekey !== map.tileFocus ) {
                // only update tilefocus if it actually changes
                map.previousTileFocus = map.tileFocus;
                map.tileFocus = tilekey;
                PubSub.publish( 'layer', { field: 'tileFocus', value: tilekey });
            }
        }
        // set tile focus callbacks
        map.on('mousemove', function( event ) {
            updateTileFocus( event.xy.x, event.xy.y );
            previousMouse.x = event.xy.x;
            previousMouse.y = event.xy.y;
        });
        map.on('zoomend', function() {
            updateTileFocus( previousMouse.x, previousMouse.y );
        });
        // if mousedown while map is panning, interrupt pan
        map.olMap.events.register( "mousedown", map, function(){
            if ( map.olMap.panTween ) {
                map.olMap.panTween.callbacks = null;
                map.olMap.panTween.stop();
            }
        }, true );
        // create resize callback
        map.resizeCallback = function() {
            map.olMap.updateSize();
        };
        // set resize callback
        $( window ).on( 'resize', map.resizeCallback );
    };

    /**
     * Activates a component.
     * @private
     *
     * @param map       {Map} The map object.
     * @param component {*}   The component to activate.
     */
    activateComponent = function( map, component ) {
        if ( component instanceof Carousel ) {
            addCarousel( map, component );
        } else if ( component instanceof Layer ) {
            if ( component.carousel ) {
                console.log(
                    "You cannot add a layer that is part of a carousel to a map " +
                    "independently, remove it from the carousel first." );
                return;
            }
            addLayer( map, component );
        } else if ( component instanceof Axis ) {
            addAxis( map, component );
        }
    };

    /**
     * Deactivates a component.
     * @private
     *
     * @param map       {Map} The map object.
     * @param component {*}   The component to deactivate.
     */
    deactivateComponent = function( map, component ) {
        if ( component instanceof BaseLayer ) {
            removeBaseLayer( map, component );
        } else if ( component instanceof Carousel ) {
            removeCarousel( map, component );
        } else if ( component instanceof Layer ) {
            removeLayer( map, component );
        } else if ( component instanceof Axis ) {
            removeAxis( map, component );
        }
    };

    /**
     * Activates deferred components when the map is ready.
     * @private
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
     * Adds a base layer to the map. If no baselayer is attached, it
     * will also activate it, along with any deferred components that were attached
     * first.
     * @private
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
            // create pending layer now
            if ( map.showPendingTiles ) {
                map.pendingLayer = new PendingLayer();
                map.pendingLayer.map = map;
                map.pendingLayer.activate();
            }
            if ( map.deferreds ) {
                activateDeferredComponents( map );
            }
        }
    };

    /**
     * Adds a layer object to the map and activates it.
     * @private
     *
     * @param map   {Map}   The map object.
     * @param layer {Layer} The layer object.
     */
    addLayer = function( map, layer ) {
        // add map to layer
        layer.map = map;
        // track layer
        if ( map.showPendingTiles && layer.showPendingTiles ) {
            map.pendingLayer.register( layer );
        }
        // activate the layer
        layer.activate();
        // add to layer array
        map.layers = map.layers || [];
        map.layers.push( layer );
        // add it to layer map
        map.layersById = map.layersById || {};
        map.layersById[ layer.getUUID() ] = layer;
    };

    /**
     * Adds a carousel object to the map and activates it.
     * @private
     *
     * @param map   {Map}   The map object.
     * @param carousel {Carousel} The carousel object.
     */
    addCarousel = function( map, carousel ) {
        // add map to carousel
        carousel.map = map;
        // activate the carousel
        carousel.activate();
        // add to carousel array
        map.carousels = map.carousels || [];
        map.carousels.push( carousel );
        // add it to carousel map
        map.carouselsById = map.carouselsById || {};
        map.carouselsById[ carousel.getUUID() ] = carousel;
    };

    /**
     * Adds an Axis object to the map and activates it.
     * @private
     *
     * @param map  {Map}  The map object.
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
            value.updateDimension();
        });
        // redraw
        _.forIn( map.axes, function( value ) {
            value.redraw();
        });
    };

    /**
     * Removes a base layer from the map. If no other baselayer is attached, it
     * will refuse to do so.
     * @private
     *
     * @param map       {Map}       The map object.
     * @param baselayer {BaseLayer} The baselayer object.
     */
    removeBaseLayer = function( map, baselayer ) {
        var index;
        // if only 1 baselayer available, ignore
        if ( !map.destroying && map.baselayers.length === 1 ) {
            console.error( 'Error: attempting to remove only baselayer from ' +
                'map, this destroys the map, use destroy() instead' );
            return;
        }
        // get index of baselayer
        index = map.baselayers.indexOf( baselayer );
         // remove baselayer from array
        map.baselayers.splice( index, 1 );
        // if we are removing an active base layer, change to
        // next index
        if ( index === map.baseLayerIndex ) {
            // get replacement index
            index = ( map.baselayers[ index ] ) ? index : index-1;
            // replace baselayer
            map.setBaseLayerIndex( index );
        } else {
            if ( index < map.baseLayerIndex ) {
                map.baseLayerIndex--;
            }
        }
        baselayer.map = null;
    };

    /**
     * Removes a layer object from the map and deactivates it.
     * @private
     *
     * @param map   {Map}   The map object.
     * @param layer {Layer} The layer object.
     */
    removeLayer = function( map, layer ) {
        var index = map.layers.indexOf( layer );
        if ( index !== -1 ) {
             // remove it from layer map
            delete map.layersById[ layer.getUUID() ];
            // remove it from layer array
            map.layers.splice( index, 1 );
            // track layer
            if ( map.showPendingTiles ) {
                map.pendingLayer.unregister( layer );
            }
            // deactivate it
            layer.deactivate();
            layer.map = null;
            // reset z-indices
            resetLayerZIndices( map );
        }
    };

    /**
     * Removes a carousel object from the map and deactivates it.
     * @private
     *
     * @param map   {Map}   The map object.
     * @param carousel {Carousel} The carousel object.
     */
    removeCarousel = function( map, carousel ) {
        var index = map.carousels.indexOf( carousel );
        if ( index !== -1 ) {
             // remove it from layer map
            delete map.carouselsById[ carousel.getUUID() ];
            // remove it from layer array
            map.carousels.splice( index, 1 );
            // deactivate it
            carousel.deactivate();
            carousel.map = null;
            // reset z-indices
            resetLayerZIndices( map );
        }
    };

    /**
     * Removes an Axis object from the map and deactivates it.
     * @private
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

    /**
     * Removing layers causes a z-index reset since we use css z-index rather
     * than OpenLayers's built in relative indexing (which sucks for dynamic maps).
     * This is used to reset all z-indices accordingly.
     * @private
     *
     * @param {Map} map - The map object.
     */
    resetLayerZIndices = function( map ) {
        var layers = map.layers,
            baselayer,
            i;
        if ( map.layers ) {
            for ( i=0; i<layers.length; i++ ) {
                layers[i].setZIndex( layers[i].getZIndex() );
            }
        }
        if ( map.baseLayerIndex >= 0 ) {
            baselayer = map.baselayers[ map.baseLayerIndex ];
            baselayer.resetZIndex();
        }
        if ( map.olMarkers ) {
            $( map.olMarkers.div ).css( 'z-index', MARKER_Z_INDEX );
        }
    };

    /**
     * Instantiate a Map object.
     * @class Map
     * @classdesc A map object that acts as a central container for all layers and other map
     *            components.
     *
     * @param {String} id - The DOM element id string.
     * @param {Object} spec - The specification object.
     * <pre>
     * {
     *     pyramid {String} - The pyramid type for the map. Defaults to 'WebMercator'
     *     options: {
     *         numZoomLevels {integer} - The number of zoom levels. Default = 18.
     *         units {integer} - The units used for the map. Default = 'm'.
     *         zoomDelay {integer} - The delay before requesting tiles on a zoom. Default = 400.
     *         moveDelay {integer} - The delay before requesting tiles on a pan. Default = 400.
     *     }
     * }
     * </pre>
     */
    function Map( id, spec ) {
        spec = spec || {};
        spec.options = spec.options || {};
        spec.theme = spec.theme || 'dark';
        // element id
        this.id = id;
        // set map tile pyramid
        this.setPyramid( spec.pyramid );
        // initialize base layer index to -1 for no baselayer
        this.baseLayerIndex = -1;
        // disable kinetic pan
        OpenLayers.Control.DragPan.prototype.enableKinetic = false;
		// navigation controls
		this.navigationControls = new OpenLayers.Control.Navigation({
			documentDrag: true,
			zoomBoxEnabled: false
		});
		// zoom controls
		this.zoomControls = new OpenLayers.Control.Zoom();
        // create map object
        this.olMap = new OpenLayers.Map( this.id, {
            theme: null, // prevent OpenLayers from checking for default css
            projection: new OpenLayers.Projection( "EPSG:900913" ),
            displayProjection: new OpenLayers.Projection( "EPSG:4326" ),
            maxExtent: OpenLayers.Bounds.fromArray([
                -20037508.342789244,
                -20037508.342789244,
                20037508.342789244,
                20037508.342789244
            ]),
            zoomMethod: null,
            units: spec.options.units || "m",
            numZoomLevels: spec.options.numZoomLevels || 18,
            fallThrough: true,
            controls: [
                this.navigationControls,
                this.zoomControls
            ],
            tileManager: OpenLayers.TileManager ? new OpenLayers.TileManager({
                moveDelay: spec.options.moveDelay !== undefined ? spec.options.moveDelay : 400,
                zoomDelay: spec.options.zoomDelay !== undefined ? spec.options.zoomDelay : 400
            }) : undefined
        });
        // show animation on pending tiles
        this.showPendingTiles = ( spec.showPendingTiles !== undefined ) ? spec.showPendingTiles : true;
        // set theme, default to 'dark' theme
        this.setTheme( spec.theme );
    }

    Map.prototype = {

        /**
         * Removes all components and destroys the map.
         * @memberof Map.prototype
         */
        destroy: function() {
            this.destroying = true;
            // remove pending layer
            if ( this.pendingLayer ) {
                this.pendingLayer.deactivate();
                this.pendingLayer.map = null;
                this.pendingLayer = null;
            }
            // remove marker layer
            if ( this.olMarkers ) {
                this.olMap.removeLayer( this.olMarkers );
                this.olMarkers = null;
            }
            this.layers.forEach( function( layer ) {
                this.remove( layer );
            }, this );
            _.forIn( this.axes, function( axis ) {
                this.remove( axis );
            }, this );
            this.baselayers.forEach( function( baselayer ) {
                this.remove( baselayer );
            }, this );
            // remove window resize callback
            $( window ).off( 'resize', this.resizeCallback );
            // destroy map
            this.olMap.destroy();
        },

        /**
         * Adds a component to the map.
         * @memberof Map.prototype
         *
         * @param {Layer|Axis} component - The component object.
         */
        add: function( component ) {
            if ( component instanceof BaseLayer ) {
                // if a baselayer, add it
                addBaseLayer( this, component );
                return;
            }
            if ( this.baseLayerIndex < 0 ) {
                // if no baselayer is attached yet, we cannot activate the component
                // add it to list of deferred activations
                this.deferreds = this.deferreds || [];
                this.deferreds.push( component );
                return;
            }
            // activate the component
            activateComponent( this, component );
        },

        /**
         * Removes a component from the map.
         * @memberof Map.prototype
         *
         * @param {Layer|Axis} component - The component object.
         */
        remove: function( component ) {
            if ( this.baseLayerIndex < 0 ) {
                // if no baselayer is attached yet, we cannot deactivate the component
                // remove it from the list of deferred activations
                this.deferreds = this.deferreds || [];
                this.deferreds.splice( this.deferreds.indexOf( component ), 1 );
                return;
            }
            // activate the component
            deactivateComponent( this, component );
        },

        /**
         * Enables the panning controls for the map.
         */
        enableNavigation: function() {
            this.navigationControls.activate();
        },

        /**
         * Disables the panning controls for the map.
         */
        disableNavigation: function() {
            this.navigationControls.deactivate();
        },

        /**
         * Returns the tilekey for the tile currently under the mouse.
         * @memberof Map.prototype
         *
         * @returns {String} The tilekey currently under the mouse.
         */
        getTileFocus: function() {
            return this.tileFocus;
        },

        /**
         * If multiple baselayers are attached to the map, this function is
         * used to change the currently active one by index.
         * @memberof Map.prototype
         *
         * @param {integer} index - The index of the baselayer to switch to.
         */
        setBaseLayerIndex: function( index ) {
            var oldBaseLayer = this.baselayers[ this.baseLayerIndex ],
                newBaseLayer = this.baselayers[ index ];
            if ( !newBaseLayer ) {
                console.error("Error, no baselayer for supplied index: " + index );
                return;
            }
            if ( oldBaseLayer === newBaseLayer ) {
                // same layer, don't switch
                return;
            }
            if ( oldBaseLayer ) {
                oldBaseLayer.deactivate();
            }
            newBaseLayer.activate();
            this.baseLayerIndex = index;
            // update z index, since changing baselayer resets them
            resetLayerZIndices( this );
            PubSub.publish( newBaseLayer.getChannel(), { field: 'baseLayerIndex', value: index });
        },

        /**
         * Returns the currently active baselayer index.
         * @memberof Map.prototype
         *
         * @returns {integer} The currently active baselayer index.
         */
        getBaseLayerIndex: function() {
            return this.baseLayerIndex;
        },

        /**
         * Returns the currently active baselayer, or null if there isn't one.
         * @memberof Map.prototype
         *
         * @returns {BaseLayer} The currently active baselayer.
         */
        getActiveBaseLayer: function() {
            if ( this.baseLayerIndex === -1 ) {
                return null;
            }
            return this.baselayers[ this.baseLayerIndex ];
        },

        /**
         * Set the theme of the map. Currently restricted to "dark" and "light".
         * @memberof Map.prototype
         *
         * @param {String} theme - The theme identification string of the map.
         */
        setTheme: function( theme ) {
            if ( this.theme === theme ) {
                return;
            }
            var i;
            // toggle theme in html
            if ( theme === 'light' ) {
                $( 'body' ).removeClass( "dark-theme" ).addClass( "light-theme" );
            } else {
                $( 'body' ).removeClass( "light-theme" ).addClass( "dark-theme" );
            }
            this.theme = theme;
            // update theme for all attached layers
            if ( this.layers ) {
                for ( i=0; i<this.layers.length; i++ ) {
                    if ( this.layers[i].setTheme ) {
                        this.layers[i].setTheme( theme );
                    }
                }
            }
        },

        /**
         * Returns the current theme of the map. Currently restricted to "dark"
         * and "light".
         * @memberof Map.prototype
         *
         * @returns {String} The theme of the map.
         */
        getTheme: function() {
            return this.theme;
        },

        /**
         * Returns the map DOM element. This is the element to which
         * the map object is 'attached'.
         * @memberof Map.prototype
         *
         * @returns {HTMLElement} The map div element.
         */
        getElement:  function() {
            return this.olMap.div;
        },

        /**
         * Returns the map viewport DOM element. This the element that encompasses
         * the viewable portion of the map.
         * @memberof Map.prototype
         *
         * @returns {HTMLElement} The map viewport div element.
         */
        getViewportElement:  function() {
            return this.olMap.viewPortDiv;
        },

        /**
         * Returns the map container DOM element. This is the element to which all
         * 'pannable' layers are attached to.
         * @memberof Map.prototype
         *
         * @returns {HTMLElement} The map container div element.
         */
        getContainerElement:  function() {
            return this.olMap.layerContainerDiv;
        },

        /**
         * Add a pyramid to the map. All Tile iterators returned prior to this
         * will be invalidated.
         * @memberof Map.prototype
         *
         * @param {AreaOfInterestTilePyramid|WebMercatorTilePyramid|Object} pyramid - The pyramid.
         */
        setPyramid: function( pyramid ) {
            if ( !pyramid ) {
                this.pyramid = new WebMercatorTilePyramid();
            } else if ( pyramid instanceof AreaOfInterestTilePyramid ||
                pyramid instanceof WebMercatorTilePyramid ) {
                this.pyramid = pyramid;
            } else if ( pyramid.type && pyramid.type.toLowerCase() === "areaofinterest" ) {
                this.pyramid = new AreaOfInterestTilePyramid( pyramid );
            } else {
                this.pyramid = new WebMercatorTilePyramid();
            }
        },

        /**
         * Returns the tile pyramid used by the map.
         * @memberof Map.prototype
         *
         * @returns {AreaOfInterestTilePyramid|WebMercatorTilePyramid} The TilePyramid object.
         */
        getPyramid: function() {
            return this.pyramid;
        },

        /**
         * Returns a TileIterator object. This TileIterator contains all tiles currently
         * visible in the map.
         * @memberof Map.prototype
         *
         * @returns {TileIterator} A TileIterator object containing all visible tiles.
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
         * Returns an array of all tilekeys currently visible in the map.
         * @memberof Map.prototype
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
         * Zooms the map to a particular coordinate and zoom level. The
         * transition is instantaneous.
         * @memberof Map.prototype
         *
         * @param {number} x - The x coordinate (longitude for geospatial).
         * @param {number} y - The y coordinate (latitude for geospatial).
         * @param {integer} zoom - The zoom level.
         */
        zoomTo: function( x, y, zoom ) {
            var viewportPx = MapUtil.getViewportPixelFromCoord( this, x, y ),
                lonlat = this.olMap.getLonLatFromViewPortPx( viewportPx );
            this.olMap.setCenter( lonlat, zoom );
        },

        /**
         * Zooms the map to a particular bounding box. The transition is
         * instantaneous.
         * @memberof Map.prototype
         *
         * @param {Object} bounds - The bounding box to zoom to.
         */
        zoomToExtent: function( bounds ) {
            var minViewportPx = MapUtil.getViewportPixelFromCoord( this, bounds.minX, bounds.minY ),
                maxViewportPx = MapUtil.getViewportPixelFromCoord( this, bounds.maxX, bounds.maxY ),
                minLonLat = this.olMap.getLonLatFromViewPortPx( minViewportPx ),
                maxLonLat = this.olMap.getLonLatFromViewPortPx( maxViewportPx ),
                olBounds = new OpenLayers.Bounds();
            olBounds.extend( minLonLat );
            olBounds.extend( maxLonLat );
            this.olMap.zoomToExtent( olBounds );
        },

        /**
         * Restricts the map extents to a particular bounding box. If no arg is
         * provided, this removes previous restrictions.
         * @memberof Map.prototype
         *
         * @param {Object} bounds - The bounding box to zoom to.
         */
        restrictExtent: function( bounds ) {
            if ( !bounds ) {
                this.olMap.restrictedExtent = null;
                this.olMap.zoomToMaxExtent();
                return;
            }
            var minViewportPx = MapUtil.getViewportPixelFromCoord( this, bounds.minX, bounds.minY ),
                maxViewportPx = MapUtil.getViewportPixelFromCoord( this, bounds.maxX, bounds.maxY ),
                minLonLat = this.olMap.getLonLatFromViewPortPx( minViewportPx ),
                maxLonLat = this.olMap.getLonLatFromViewPortPx( maxViewportPx ),
                olBounds = new OpenLayers.Bounds();
            olBounds.extend( minLonLat );
            olBounds.extend( maxLonLat );
            this.olMap.restrictedExtent = olBounds;
            this.olMap.zoomToExtent( olBounds );
        },

        /**
         * Pans the map to a particular coordinate. The transition is
         * animated if the region is currently in view, instantaneous if
         * not.
         * @memberof Map.prototype
         *
         * @param {number} x - The x coordinate (longitude for geospatial).
         * @param {number} y - The y coordinate (latitude for geospatial).
         */
        panTo: function( x, y ) {
            var viewportPx = MapUtil.getViewportPixelFromCoord( this, x, y ),
                lonlat = this.olMap.getLonLatFromViewPortPx( viewportPx );
            this.olMap.panTo( lonlat );
        },


        /**
         * Creates a marker at the specified position of the map.
         * @memberof Map.prototype
         *
         * @param {number} x - The x coordinate (longitude for geospatial).
         * @param {number} y - The y coordinate (latitude for geospatial).
         * @param {Marker} marker - The Marker object to add.
         *
         * @returns {Marker} The added marker.
         */
        addMarker: function( x, y, marker ) {
            if ( !this.olMarkers ) {
                this.olMarkers = new OpenLayers.Layer.Markers( "Markers" );
                this.olMap.addLayer( this.olMarkers );
            }
            // always update the z-index of this div
            $( this.olMarkers.div ).css( 'z-index', 5000 );
            marker.map = this;
            marker.activate( x, y );
            return marker;
        },

        /**
         * Removes a marker from the map.
         *
         * @param {OpenLayers.Marker} marker - The marker object.
         */
        removeMarker: function( marker ) {
            if ( this.olMarkers ) {
                marker.deactivate();
                marker.map = null;
            }
        },

        /**
         * Removes all markers from the map.
         */
        clearMarkers: function() {
            if ( this.olMarkers ) {
                this.olMarkers.clearMarkers();
            }
        },

        /**
         * Returns the width of the entire map in pixels.
         * @memberof Map.prototype
         *
         * @returns {integer} The width of the map in pixels.
         */
        getWidth: function() {
            return TILESIZE * Math.pow( 2, this.getZoom() );
        },

        /**
         * Returns the height of the entire map in pixels.
         * @memberof Map.prototype
         *
         * @returns {integer} The height of the map in pixels.
         */
        getHeight: function() {
            return TILESIZE * Math.pow( 2, this.getZoom() );
        },

        /**
         * Returns the width of the viewport in pixels.
         * @memberof Map.prototype
         *
         * @returns {integer} The width of the viewport in pixels.
         */
        getViewportWidth: function() {
            return this.olMap.viewPortDiv.clientWidth;
        },

        /**
         * Returns the height of the viewport in pixels.
         * @memberof Map.prototype
         *
         * @returns {integer} The height of the viewport in pixels.
         */
        getViewportHeight: function() {
            return this.olMap.viewPortDiv.clientHeight;
        },

        /**
         * Returns the maps current zoom level. Level 0 is contains the most
         * of aggregation.
         * @memberof Map.prototype
         *
         * @returns {integer} The zoom level.
         */
        getZoom: function () {
            return this.olMap.getZoom();
        },

        /**
         * Returns the x and y coordinates at the centre of the map.
         *
         * @return {OpenLayers.LonLat}
         */
        getCenterProjected: function() {
            return MapUtil.getCoordFromViewportPixel(
                this,
                this.getViewportWidth() / 2,
                this.getViewportHeight() / 2 );
        },

        /**
         * Get the top-left and bottom-right extents of the visible map.
         *
         * @return {Object}
         */
        getMapExtents: function() {
            return {
                topLeft: MapUtil.getCoordFromViewportPixel(
                    this,
                    0,
                    0 ),
                bottomRight: MapUtil.getCoordFromViewportPixel(
                    this,
                    this.getViewportWidth(),
                    this.getViewportHeight() )
            };
        },

        /**
         * Set a map event callback. Supports all of the following OpenLayers.Map events:
         * <pre>
         *     movestart - triggered after the start of a drag, pan, or zoom.  The event object may include a zoomChanged property that tells whether the zoom has changed.
         *     move - triggered after each drag, pan, or zoom
         *     moveend - triggered after a drag, pan, or zoom completes
         *     zoomstart - triggered when a zoom starts.  Listeners receive an object with center and zoom properties, for the target center and zoom level.
         *     zoomend - triggered after a zoom completes
         *     mouseover - triggered after mouseover the map
         *     mouseout - triggered after mouseout the map
         *     mousemove - triggered after mousemove the map
         * </pre>
         * @memberof Map.prototype
         *
         * @param {String} eventType - The event type.
         * @param {Function} callback - The callback.
         */
        on: function( eventType, callback ) {
            this.olMap.events.register( eventType, this.olMap, callback );
        },

        /**
         * Remove a map event callback. Supports all of the following OpenLayers.Map events:
         * <pre>
         *     movestart - triggered after the start of a drag, pan, or zoom.  The event object may include a zoomChanged property that tells whether the zoom has changed.
         *     move - triggered after each drag, pan, or zoom
         *     moveend - triggered after a drag, pan, or zoom completes
         *     zoomstart - triggered when a zoom starts.  Listeners receive an object with center and zoom properties, for the target center and zoom level.
         *     zoomend - triggered after a zoom completes
         *     mouseover - triggered after mouseover the map
         *     mouseout - triggered after mouseout the map
         *     mousemove - triggered after mousemove the map
         * </pre>
         * @memberof Map.prototype
         *
         * @param {String} eventType - The event type.
         * @param {Function} callback - The callback.
         */
        off: function( eventType, callback ) {
            this.olMap.events.unregister( eventType, this.olMap, callback );
        },

        /**
         * Trigger a map event. Supports all of the following OpenLayers.Map events:
         * <pre>
         *     movestart - triggered after the start of a drag, pan, or zoom.  The event object may include a zoomChanged property that tells whether the zoom has changed.
         *     move - triggered after each drag, pan, or zoom
         *     moveend - triggered after a drag, pan, or zoom completes
         *     zoomstart - triggered when a zoom starts.  Listeners receive an object with center and zoom properties, for the target center and zoom level.
         *     zoomend - triggered after a zoom completes
         *     mouseover - triggered after mouseover the map
         *     mouseout - triggered after mouseout the map
         *     mousemove - triggered after mousemove the map
         * </pre>
         * @memberof Map.prototype
         *
         * @param {String} eventType - The event type.
         * @param {Object} event - The event object to be passed to the event.
         */
        trigger: function( eventType, event ) {
            this.olMap.events.triggerEvent( eventType, event );
        }
    };

    module.exports = Map;
}());
