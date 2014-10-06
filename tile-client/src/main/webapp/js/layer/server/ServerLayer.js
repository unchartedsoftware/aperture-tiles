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
        PubSub = require('../../util/PubSub'),
        requestRampImage,
        getLevelMinMax,
        mapZoomCallback,
        ServerLayer;



    /**
     * Request colour ramp image.
     *
     * @param {Object} layer - The layer object
     */
    requestRampImage = function ( layer, layerInfo, level ) {

        var legendData = {
                layer: layer.id,  // layer id
                id: layerInfo.id, // config uuid
                level: level,
                width: 128,
                height: 1,
                orientation: "horizontal"
            };

        aperture.io.rest('/legend',
                         'POST',
                         function (legendString, status) {
                            layer.setRampImageUrl( legendString );
                         },
                         {
                             postData: legendData,
                             contentType: 'application/json'
                         });

    };


    getLevelMinMax = function( layer, map ) {
        var zoomLevel = map.getZoom(),
            coarseness = layer.getLayerSpec().coarseness,
            adjustedZoom = zoomLevel - (coarseness-1),
            meta =  layer.getLayerInfo().meta,
            minArray = (meta && (meta.levelMinFreq || meta.levelMinimums || meta[adjustedZoom])),
            maxArray = (meta && (meta.levelMaxFreq || meta.levelMaximums || meta[adjustedZoom])),
            min = minArray ? ($.isArray(minArray) ? minArray[adjustedZoom] : minArray.minimum) : 0,
            max = maxArray ? ($.isArray(maxArray) ? maxArray[adjustedZoom] : maxArray.maximum) : 0;
        return [ parseFloat(min), parseFloat(max) ];
    };


    // Make a callback to regen the ramp image on map zoom changes
    mapZoomCallback = function( layer, map ) {
        if ( layer ) {
            // set ramp image
            requestRampImage( layer, layer.getLayerInfo(), map.getZoom() );
            // set ramp level
            layer.setRampMinMax( getLevelMinMax( layer, map ) );
        }
    };



    ServerLayer = Layer.extend({
        ClassName: "ServerLayer",


        /**
         * Valid ramp type strings.
         */
        RAMP_TYPES: [
            {id: "ware", name: "Luminance"},
            {id: "inv-ware", name: "Inverse Luminance"},
            {id: "br", name: "Blue/Red"},
            {id: "inv-br", name: "Inverse Blue/Red"},
            {id: "grey", name: "Grey"},
            {id: "inv-grey", name: "Inverse Grey"},
            {id: "flat", name: "Flat"},
            {id: "single-gradient", name: "Single Gradient"}
        ],


        /**
         * Valid ramp function strings.
         */
        RAMP_FUNCTIONS: [
            {id: "linear", name: "Linear"},
            {id: "log10", name: "Log 10"}
        ],


        init: function ( spec, map ) {

            var that = this;

            // set reasonable defaults
            spec.renderer.opacity  = spec.renderer.opacity || 1.0;
            spec.renderer.enabled = ( spec.renderer.enabled !== undefined ) ? spec.renderer.enabled : true;
            spec.renderer.ramp = spec.renderer.ramp || "ware";
            spec.transform.name = spec.transform.name || 'linear';
            spec.legendrange = spec.legendrange || [0,100];
            spec.transformer = spec.transformer || {};
            spec.transformer.type = spec.transformer.type || "generic";
            spec.transformer.data = spec.transformer.data || {};
            spec.coarseness = ( spec.coarseness !== undefined ) ?  spec.coarseness : 1;

            // cal base constructor
            this._super( spec, map );

            this.map.on("zoomend", function() {
                mapZoomCallback( that, that.map );
            });

            this.hasBeenConfigured = false;
        },


        /**
         * Set the opacity
         */
        setOpacity: function ( opacity ) {
            if (this.layer) {
                this.layer.olLayer_.setOpacity( opacity );
                PubSub.publish( this.getChannel(), { field: 'opacity', value: opacity });
            }
        },


        getOpacity: function() {
            return this.layer.olLayer_.opacity;
        },


        /**
         * Set the visibility
         */
        setVisibility: function ( visibility ) {
            if (this.layer) {
                this.layer.olLayer_.setVisibility( visibility );
                PubSub.publish( this.getChannel(), { field: 'visibility', value: visibility });
            }
        },


        getVisibility: function() {
            return this.layer.olLayer_.getVisibility();
        },


        /**
         * Updates the ramp type associated with the layer.  Results in a POST
         * to the server.
         *
         * @param {string} rampType - The new ramp type for the layer.
         * @param {function} callback - The callback function when the configure request returns, used to request new
         *                              ramp image
         */
        setRampType: function ( rampType ) {

            var that = this;
            if ( !this.layerSpec.renderer ) {
                this.layerSpec.renderer = {ramp: rampType};
            } else {
                this.layerSpec.renderer.ramp = rampType;
            }
            this.configure( function() {
                // once configuration is received that the server has been re-configured, request new image
                requestRampImage( that, that.getLayerInfo(), that.map.getZoom() );
                that.update();
            });
        },


        getRampType: function() {
            return this.layerSpec.renderer.ramp;
        },


        invertRampType: function() {
            // default to inv-ware
            if ( $("body").hasClass("light-theme") ) {
                this.setRampType('inv-ware');
            } else {
                this.setRampType('ware');
            }
        },


        setRampMinMax: function( minMax ) {
            this.rampMinMax = minMax;
            PubSub.publish( this.getChannel(), { field: 'rampMinMax', value: minMax });
        },


        getRampMinMax: function() {
            return this.rampMinMax;
        },


        setRampImageUrl: function ( url ) {
            this.rampImageUrl = url;
            PubSub.publish( this.getChannel(), { field: 'rampImageUrl', value: url });
        },


        getRampImageUrl: function() {
            return this.rampImageUrl;
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
            PubSub.publish( this.getChannel(), { field: 'rampFunction', value: rampFunction });
        },


        getRampFunction: function() {
            return this.layerSpec.transform.name;
        },


        /**
         * Updates the filter range for the layer.  Results in a POST to the server.
         *
         * @param {Array} filterRange - A two element array with values in the range [0.0, 1.0],
         * where the first element is the min range, and the second is the max range.
         */
        setFilterRange: function ( filterRange ) {
            this.layerSpec.legendrange = filterRange;
            this.configure( $.proxy( this.update, this ) );
            PubSub.publish( this.getChannel(), { field: 'filterRange', value: filterRange });
        },


        getFilterRange: function() {
            return this.layerSpec.legendrange;
        },


        /**
         * @param {number} zIndex - The new z-order value of the layer, where 0 is front.
         */
        setZIndex: function ( zIndex ) {
            this.map.setLayerIndex( this.layer.olLayer_, zIndex );
            PubSub.publish( this.getChannel(), { field: 'zIndex', value: zIndex });
        },


        getZIndex: function () {
            return this.map.getLayerIndex( this.layer.olLayer_ );
        },


        setTransformerType: function ( transformerType ) {
            this.layerSpec.transformer.type = transformerType;
            this.configure( $.proxy( this.update, this ) );
            PubSub.publish( this.getChannel(), { field: 'transformerType', value: transformerType });
        },


        getTransformerType: function () {
            return this.layerSpec.transformer.type;
        },


        setTransformerData: function ( transformerData ) {
            this.layerSpec.transformer.data = transformerData;
            this.configure( $.proxy( this.update, this ) );
            PubSub.publish( this.getChannel(), { field: 'transformerData', value: transformerData });
        },


        getTransformerData: function () {
            return this.layerSpec.transformer.data;
        },


        setCoarseness: function( coarseness ) {
            this.layerSpec.coarseness = coarseness;
            this.configure( $.proxy( this.update, this ) );
            this.setRampMinMax( getLevelMinMax( this, this.map ) );
            PubSub.publish( this.getChannel(), { field: 'coarseness', value: coarseness });
        },


        getCoarseness: function() {
            return this.layerSpec.coarseness;
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
                if ( that.hasBeenConfigured === false ) {
                    requestRampImage( that, that.getLayerInfo(), 0 );
                    that.setZIndex( that.layerSpec.zIndex );
                    that.setVisibility( that.layerSpec.renderer.enabled );
                    that.setCoarseness( that.layerSpec.coarseness );
                    that.hasBeenConfigured = true;
                }
            });
        },


        /**
         * Update all our openlayers layers on our map.
         */
        update: function () {

            var that = this,
                olBounds,
                yFunction;

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

            if ( !this.layer ) {

                // add the new layer
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

            } else {

                // layer already exists, simply update service endpoint
                this.layer.olLayer_.getURL = createUrl;
                this.layer.olLayer_.url[0] = this.layerInfo.tms;
            }

            // redraw this layer
            this.layer.olLayer_.redraw();
        }
    });

    return ServerLayer;
});
