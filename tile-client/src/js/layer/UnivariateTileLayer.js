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

    var UnivariateTile = require('./UnivariateTile');

    function clampBounds( bounds, layer ) {
        bounds = bounds.clone();
        if ( layer.dimension === "x" ) {
            bounds.bottom = layer.map.getMaxExtent().bottom;
            bounds.top = layer.map.getMaxExtent().bottom + 1;
        } else {
            bounds.left = layer.map.getMaxExtent().left;
            bounds.right = layer.map.getMaxExtent().left + 1;
        }
        return bounds;
    }

    function repositionLayer( map, layer ) {
        var $viewport = $( map.viewPortDiv ),
            $div = $( layer.div ),
            height = $viewport.height();
        if ( layer.dimension === "x" )  {
            $div.css( 'top', height );
        } else {
            $div.css( 'left', 0 );
        }
        updateTilePositions(map, layer);
    }

    function onMapMove( map, layer ) {
        return function() {
            updateTilePositions(map, layer);
        };
    }

    function updateTilePositions(map, layer) {
        var $container = $( layer.div ).parent(),
            $viewport = $( map.viewPortDiv ),
            $div = $( layer.div ),
            offset = $container.position(),
            height = $viewport.height();
        if ( layer.dimension === "x" ) {
            // reset tile position within the layer div
            $div.children().each( function() {
                $( this ).css( 'top', -256 );
            });
            // then set div position
            $div.css( 'top', height - offset.top );
        } else {
            // reset tile position within the layer div
            $div.children().each( function() {
                $( this ).css( 'left', 0 );
            });
            // then set div position
            $div.css( 'left', -offset.left );
        }
    }

    OpenLayers.Layer.Univariate = function( name, url, options ) {
        OpenLayers.Layer.Grid.call( this, name, url, options );
        this.getURL = options.getURL;
        this.layername = options.layername;
        this.dimension = options.dimension || 'x';
        this.type = options.type;
        this.tileClass = options.tileClass || UnivariateTile;
        this.html = options.html;
        this.renderer = options.renderer;
        this.CLASS_NAME = 'OpenLayers.Layer.Univariate';
    };

    OpenLayers.Layer.Univariate.prototype = Object.create( OpenLayers.Layer.Grid.prototype );

    /**
     * Override to set all children the opacity.
     */
    OpenLayers.Layer.Univariate.prototype.setOpacity = function( opacity ) {
        if ( opacity !== this.opacity ) {
            this.opacity = Math.max( Math.min( opacity, 1 ), 0 );
            var childNodes = this.div.childNodes;
            for( var i = 0, len = childNodes.length; i < len; ++i ) {
                childNodes[i].style.opacity = this.opacity;
            }
            if ( this.map !== null ) {
                this.map.events.triggerEvent( "changelayer", {
                    layer: this,
                    property: "opacity"
                });
            }
        }
    };

    /**
     * Override this method to add a positioning function to clamp
     * the layers tiles to the appropriate axis.
     */
    OpenLayers.Layer.Univariate.prototype.setMap = function( map ) {
        OpenLayers.Layer.Grid.prototype.setMap.apply( this, arguments );
        this.onMapMove = onMapMove( map, this );
        map.events.register( 'move', map, this.onMapMove );
        repositionLayer( map, this ); // set position
    };

    /**
     * Override this method to remove the positioning function to clamp
     * the layers tiles to the appropriate axis.
     */
    OpenLayers.Layer.Univariate.prototype.removeMap = function( map ) {
        OpenLayers.Layer.Grid.prototype.removeMap.apply( this, arguments );
        map.events.unregister( 'move', map, this.onMapMove );
        this.onMapMove = null;
    };

    /**
     * Override this method to re-position the layer and prevent a redraw which
     * will reposition the tiles.
     */
    OpenLayers.Layer.Univariate.prototype.setVisibility = function( visibility ) {
        OpenLayers.Layer.Grid.prototype.setVisibility.apply( this, arguments );
        if ( this.onMapMove && visibility ) {
            repositionLayer( this.map, this );
        }
    };

    OpenLayers.Layer.Univariate.prototype.getExtent = function() {
        return clampBounds( this.map.calculateBounds(), this );
    };

    OpenLayers.Layer.Univariate.prototype.redraw = function() {
        var redrawn = false;
        if (this.map) {
            // min/max Range may have changed
            this.inRange = this.calculateInRange();
            // map's center might not yet be set
            var extent = this.getExtent();
            if ( extent && this.inRange && this.visibility ) {
                var zoomChanged = true;
                this.moveTo( extent, zoomChanged, false );
                this.events.triggerEvent("moveend",
                    {"zoomChanged": zoomChanged});
                redrawn = true;
                repositionLayer( this.map, this );
            }
        }
        return redrawn;
    };

    /**
     * Override this method to clamp the 'maxExtents' value returned from the
     * map object.
     */
    OpenLayers.Layer.Univariate.prototype.moveTo = function( bounds, zoomChanged, dragging ) {

        if ( !bounds ) {
            bounds = this.map.getExtent();
        }

        if ( bounds !== null ) {

            bounds = clampBounds( bounds, this );

            // if grid is empty or zoom has changed, we *must* re-tile
            var forceReTile = !this.grid.length || zoomChanged;

            // total bounds of the tiles
            var tilesBounds = this.getTilesBounds();

            // the new map resolution
            var resolution = this.map.getResolution();

            // the server-supported resolution for the new map resolution
            var serverResolution = this.getServerResolution(resolution);

            if ( this.singleTile ) {

                // We want to redraw whenever even the slightest part of the
                //  current bounds is not contained by our tile.
                //  (thus, we do not specify partial -- its default is false)

                if ( forceReTile ||
                     ( !dragging && !tilesBounds.containsBounds( bounds ) ) ) {

                    // In single tile mode with no transition effect, we insert
                    // a non-scaled backbuffer when the layer is moved. But if
                    // a zoom occurs right after a move, i.e. before the new
                    // image is received, we need to remove the backbuffer, or
                    // an ill-positioned image will be visible during the zoom
                    // transition.

                    if ( zoomChanged && this.transitionEffect !== 'resize' ) {
                        this.removeBackBuffer();
                    }

                    if ( !zoomChanged || this.transitionEffect === 'resize' ) {
                        this.applyBackBuffer( serverResolution );
                    }

                    this.initSingleTile( bounds );
                }
            } else {

                // if the bounds have changed such that they are not even
                // *partially* contained by our tiles (e.g. when user has
                // programmatically panned to the other side of the earth on
                // zoom level 18), then moveGriddedTiles could potentially have
                // to run through thousands of cycles, so we want to reTile
                // instead (thus, partial true).
                forceReTile = forceReTile ||
                    !tilesBounds.intersectsBounds( bounds, {
                        worldBounds: this.map.baseLayer.wrapDateLine &&
                            clampBounds( this.map.getMaxExtent(), this )
                    });

                if ( resolution !== serverResolution ) {
                    bounds = this.map.calculateBounds( null, serverResolution );
                    bounds = clampBounds( bounds, this );
                    if( forceReTile ) {
                        // stretch the layer div
                        var scale = serverResolution / resolution;
                        this.transformDiv( scale );
                    }
                } else {
                    // reset the layer width, height, left, top, to deal with
                    // the case where the layer was previously transformed
                    this.div.style.width = '100%';
                    this.div.style.height = '100%';
                    this.div.style.left = '0%';
                    this.div.style.top = '0%';
                }

                if ( forceReTile ) {
                    if ( zoomChanged && this.transitionEffect === 'resize' ) {
                        this.applyBackBuffer( serverResolution );
                    }
                    this.initGriddedTiles( bounds );
                } else {
                    this.moveGriddedTiles();
                }
            }
        }
    };

    /**
     * Override this method to only shift the appropriate row or column
     * of the grid depending the the dimension this layer represents.
     */
    OpenLayers.Layer.Univariate.prototype.moveGriddedTiles = function() {
        var buffer = this.buffer + 1;
        while(true) {
            var tlTile = this.grid[0][0];
            var tlViewPort = {
                x: tlTile.position.x +
                this.map.layerContainerOriginPx.x,
                y: tlTile.position.y +
                this.map.layerContainerOriginPx.y
            };
            var ratio = this.getServerResolution() / this.map.getResolution();
            var tileSize = {
                w: Math.round(this.tileSize.w * ratio),
                h: Math.round(this.tileSize.h * ratio)
            };
            if (this.dimension === 'x') {
                if (tlViewPort.x > -tileSize.w * (buffer - 1)) {
                    this.shiftColumn(true, tileSize);
                } else if (tlViewPort.x < -tileSize.w * buffer) {
                    this.shiftColumn(false, tileSize);
                } else {
                    break;
                }
            } else {
                if (tlViewPort.y > -tileSize.h * (buffer - 1)) {
                    this.shiftRow(true, tileSize);
                } else if (tlViewPort.y < -tileSize.h * buffer) {
                    this.shiftRow(false, tileSize);
                } else {
                    break;
                }
            }
        }
    };
    module.exports = OpenLayers.Layer.Univariate;
}());
