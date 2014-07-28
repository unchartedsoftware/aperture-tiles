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



    var Layer = require('../Layer'),
        LayerService = require('../LayerService'),
        ServerLayer;



    ServerLayer = Layer.extend({
        ClassName: "ServerLayer",


        init: function ( spec, map ) {

            this._super( spec, map );
        },

        /**
         * Set the opacity
         */
        setOpacity: function ( opacity ) {
            if (this.layer) {
                this.layer.olLayer_.setOpacity( opacity );
            }
        },


        /**
         * Set the visibility
         */
        setVisibility: function ( visibility ) {
            if (this.layer) {
                this.layer.olLayer_.setVisibility( visibility );
            }
        },

        /**
         * Updates the ramp type associated with the layer.  Results in a POST
         * to the server.
         *
         * @param {string} rampType - The new ramp type for the layer.
         * @param {function} callback - The callback function when the configure request returns, used to request new
         *                              ramp image
         */
        setRampType: function ( rampType, callback ) {

            var that = this;
            if ( !this.layerSpec.renderer ) {
                this.layerSpec.renderer = {ramp: rampType};
            } else {
                this.layerSpec.renderer.ramp = rampType;
            }
            this.configure( function() {
                callback();
                that.update();
            });
        },


        /**
         * Updates the ramp function associated with the layer.  Results in a POST
         * to the server.
         *
         * @param {string} rampFunction - The new new ramp function.
         */
        setRampFunction: function ( rampFunction ) {

            if ( !this.layerSpec.transform ) {
                this.layerSpec.transform = {name: rampFunction};
            } else {
                this.layerSpec.transform.name = rampFunction;
            }
            this.configure( $.proxy( this.update, this ) );
        },


        /**
         * Updates the filter range for the layer.  Results in a POST to the server.
         *
         * @param {Array} filterRange - A two element array with values in the range [0.0, 1.0],
         * where the first element is the min range, and the second is the max range.
         */
        setFilterRange: function ( filterRange ) {
            this.layerSpec.legendrange = [filterRange[0] * 100, filterRange[1] * 100];
            this.configure( $.proxy( this.update, this ) );
        },


        /**
         * @param {number} zIndex - The new z-order value of the layer, where 0 is front.
         */
        setZIndex: function ( zIndex ) {
            this.map.setLayerIndex( this.layer.olLayer_, zIndex );
        },


        setTransformerType: function ( transformerType ) {
            this.layerSpec.transformer.type = transformerType;
            this.configure( $.proxy( this.update, this ) );
        },


        setTransformerData: function ( transformerData ) {
            this.layerSpec.transformer.data = transformerData;
            this.configure( $.proxy( this.update, this ) );
        },


        configure: function( callback ) {

            var that = this;

            LayerService.configureLayer( this.layerSpec, function( layerInfo, statusInfo ) {
                if (statusInfo.success) {
                    if ( that.layerInfo ) {
                        // if a previous configuration exists, release it
                        LayerService.unconfigureLayer( that.layerInfo, function() {
                            return true;
                        });
                    }
                    // set layer info
                    that.layerInfo = layerInfo;
                }

                callback( layerInfo, statusInfo );
            });
        },


        /**
         * Update all our openlayers layers on our map.
         */
        update: function () {

            var that = this,
                olBounds,
                yFunction,
                previousZIndex = null,
                previousOpacity = null,
                previousVisibility = null;

            // y transformation function for non-density strips
            function passY( yInput ) {
                return yInput;
            }
            // y transformation function for density strips
            function clampY( yInput ) {
                return 0;
            }
            // create url function
            function createUrl( bounds ) {

                var res = this.map.getResolution(),
                    maxBounds = this.maxExtent,
                    tileSize = this.tileSize,
                    x = Math.round( (bounds.left-maxBounds.left) / (res*tileSize.w) ),
                    y = yFunction( Math.round( (bounds.bottom-maxBounds.bottom) / (res*tileSize.h) ) ),
                    z = this.map.getZoom(),
                    fullUrl, viewBounds;

                if (x >= 0 && y >= 0) {
                    // tile bounds in view
                    viewBounds = that.map.getTileBoundsInView();
                    // set base url
                    fullUrl = (this.url + this.version + "/" +
                               this.layername + "/" +
                               z + "/" + x + "/" + y + "." + this.type);
                    // if bounds supplied, append those
                    if (viewBounds) {
                        fullUrl = (fullUrl
                                + "?minX=" + viewBounds.minX
                                + "&maxX=" + viewBounds.maxX
                                + "&minY=" + viewBounds.minY
                                + "&maxY=" + viewBounds.maxY
                                + "&minZ=" + viewBounds.minZ
                                + "&maxZ=" + viewBounds.maxZ);
                    }
                    return fullUrl;
                }
            }

            if ( !this.map || !this.layerInfo ) {
                return;
            }

            // The bounds of the full OpenLayers map, used to determine
            // tile coordinates
            // Note: these values are not set to full 20037508.342789244 as
            // it produces extra tiles around intended border. This rounded
            // down value results in the intended tile bounds ending at the
            // border of the map
            olBounds = new OpenLayers.Bounds(-20037500, -20037500,
                                              20037500,  20037500);

            // Adjust y function if we're displaying a density strip
            yFunction = ( this.layerSpec.isDensityStrip ) ? clampY : passY;

            // Remove any old version of this layer
            if ( this.layer ) {
                previousZIndex = this.map.getLayerIndex( this.layer.olLayer_ );
                previousOpacity = this.layer.olLayer_.opacity;
                previousVisibility = this.layer.olLayer_.visibility;
                this.layer.remove();
                this.layer = null;
            }

            // Add the new layer
            this.layer = this.map.addApertureLayer(
                aperture.geo.MapTileLayer.TMS, {},
                {
                    'name': 'Aperture Tile Layers',
                    'url': this.layerInfo.tms,
                    'options': {
                        'layername': this.layerInfo.layer,
                        'type': 'png',
                        'version': '1.0.0',
                        'maxExtent': olBounds,
                        transparent: true,
                        getURL: createUrl
                    }
                }
            );

            if ( previousZIndex !== null ) {
                // restore previous index
                this.map.setLayerIndex( this.layer.olLayer_, previousZIndex );
            }

            if ( previousOpacity !== null ) {
                // restore previous opacity
                this.layer.olLayer_.setOpacity( previousOpacity );
            }

            if ( previousVisibility !== null ) {
                // restore previous visibility
                this.layer.olLayer_.setVisibility( previousVisibility );
            }
        }
    });

    return ServerLayer;
});
