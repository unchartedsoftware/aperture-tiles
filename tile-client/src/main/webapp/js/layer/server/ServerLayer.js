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
        PubSub = require('../../util/PubSub'),
        requestRampImage,
        getLevelMinMax,
        ServerLayer;



    /**
     * Request colour ramp image from server.
     *
     * @param {Object} layer - The layer object.
     * @param {Object} level - The current map zoom level.
     */
    requestRampImage = function ( layer ) {

        function generateQueryParamString() {
            var query = {
                    renderer: layer.layerSpec.renderer,
                    theme: layer.layerSpec.theme
                };
            return '?'+encodeURIComponent( JSON.stringify( query ) );
        }

        aperture.io.rest('/legend/' + layer.layerSpec.source.id + generateQueryParamString(),
                         'GET',
                         function ( legendString, status ) {
                             layer.setRampImageUrl( legendString );
                         });
    };

    /**
     * Returns the layers min and max values for the given zoom level.
     *
     * @param {Object} layer - The layer object.
     * @return {Array} a two component array of the min and max values for the level.
     */
    getLevelMinMax = function( layer ) {
        var zoomLevel = layer.map.getZoom(),
            coarseness = layer.layerSpec.renderer.coarseness,
            adjustedZoom = zoomLevel - (coarseness-1),
            meta =  layer.layerSpec.source.meta.meta,
            minArray = (meta && (meta.levelMinFreq || meta.levelMinimums || meta[adjustedZoom])),
            maxArray = (meta && (meta.levelMaxFreq || meta.levelMaximums || meta[adjustedZoom])),
            min = minArray ? ($.isArray(minArray) ? minArray[adjustedZoom] : minArray.minimum) : 0,
            max = maxArray ? ($.isArray(maxArray) ? maxArray[adjustedZoom] : maxArray.maximum) : 0;
        return [ parseFloat(min), parseFloat(max) ];
    };



    ServerLayer = Layer.extend({
        ClassName: "ServerLayer",


        /**
         * Valid ramp type strings.
         */
        RAMP_TYPES: [
             {id: "spectral", name: "Spectral"},
             {id: "hot", name: "Hot"},
             {id: "polar", name: "Polar"},
             {id: "neutral", name: "Neutral"},
             {id: "cool", name: "Cool"},
             {id: "valence", name: "Valence"},
             {id: "flat", name: "Flat"}
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
            spec.opacity = ( spec.opacity !== undefined ) ? spec.opacity : 1.0;
            spec.enabled = ( spec.enabled !== undefined ) ? spec.enabled : true;
            spec.renderer = spec.renderer || {};
            spec.renderer.coarseness = ( spec.renderer.coarseness !== undefined ) ?  spec.renderer.coarseness : 1;
            spec.renderer.ramp = spec.ramp || "spectral";
            spec.renderer.rangeMin = ( spec.renderer.rangeMin !== undefined ) ? spec.renderer.rangeMin : 0;
            spec.renderer.rangeMax = ( spec.renderer.rangeMax !== undefined ) ? spec.renderer.rangeMax : 100;
            spec.renderer.preserveRangeMin = spec.preserveRangeMin || false;
            spec.renderer.preserveRangeMax = spec.preserveRangeMax || false;
            spec.renderer.theme = spec.renderer.theme || map.getTheme();
            spec.valueTransform = spec.valueTransform || { type: 'linear' };
            spec.tileTransform = spec.tileTransform || { type: 'generic' };

            // call base constructor
            this._super( spec, map );

            // update ramp min/max on zoom
            this.map.on("zoomend", function() {
                if ( that.layer ) {
                    that.setRampMinMax( getLevelMinMax( that ) );
                }
            });

            this.update();
            requestRampImage( this );
            this.setZIndex( this.layerSpec.zIndex );
            this.setOpacity( this.layerSpec.opacity );
            this.setVisibility( this.layerSpec.enabled );
            this.setRampMinMax( getLevelMinMax( that ) );
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


        /**
         *  Get layer opacity
         */
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


        /**
         * Get layer visibility
         */
        getVisibility: function() {
            return this.layer.olLayer_.getVisibility();
        },


        /**
         * Updates the ramp type associated with the layer
         */
        setRampType: function ( rampType ) {
            var that = this;
            this.layerSpec.renderer.ramp = rampType;
            requestRampImage( that );
            this.update();
        },

        /**
         *  Get ramp type for layer
         */
        getRampType: function() {
            return this.layerSpec.renderer.ramp;
        },

        /**
         * Updates the theme associated with the layer
         */
        updateTheme: function () {
        	var that = this;
            this.layerSpec.renderer.theme = this.map.getTheme();
            requestRampImage( that );
            this.update();
        },
        
        /**
         * Get the current theme for the layer
         */
        getTheme: function() {
        	return this.layerSpec.renderer.theme;
        },


        /**
         * Sets the ramps current min and max
         */
        setRampMinMax: function( minMax ) {
            this.rampMinMax = minMax;
            PubSub.publish( this.getChannel(), { field: 'rampMinMax', value: minMax });
        },


        /**
         * Get the ramps current min and max for the zoom level
         */
        getRampMinMax: function() {
            return this.rampMinMax;
        },


        /**
         * Update the ramps URL string
         */
        setRampImageUrl: function ( url ) {
            this.rampImageUrl = url;
            PubSub.publish( this.getChannel(), { field: 'rampImageUrl', value: url });
        },


        /**
         * Get the current ramps URL string
         */
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
            this.layerSpec.valueTransform = { type: rampFunction };
            this.update();
            PubSub.publish( this.getChannel(), { field: 'rampFunction', value: rampFunction });
        },


        /**
         * Get the current ramps function
         */
        getRampFunction: function() {
            return this.layerSpec.valueTransform.type;
        },


        /**
         * Updates the filter range for the layer.  Results in a POST to the server.
         *
         * @param {Array} filterRange - A two element array with values in the range [0.0, 1.0],
         * where the first element is the min range, and the second is the max range.
         */
        setFilterRange: function ( min, max ) {
            this.layerSpec.renderer.rangeMin = min;
            this.layerSpec.renderer.rangeMax = max;
            this.update();
            PubSub.publish( this.getChannel(), { field: 'filterRange', value: [ min, max ] });
        },


        /**
         * Get the current ramp filter range from 0 to 100
         */
        getFilterRange: function() {
            return [ this.layerSpec.renderer.rangeMin, this.layerSpec.renderer.rangeMax ];
        },


        /**
         * Updates the filter values for the layer.  Results in a POST to the server.
         *
         * @param {Array} filterRange - A two element array with values in the range levelMin and levelMax,
         * where the first element is the min range, and the second is the max range.
         */
        setFilterValues: function ( min, max ) {
            this.filterValues = [ min, max ];
            PubSub.publish( this.getChannel(), { field: 'filterValues', value: [ min, max ] });
        },


        /**
         * Get the current ramp filter values from levelMin and levelMax
         */
        getFilterValues: function() {
            return this.filterValues || this.getRampMinMax();
        },


        /**
         * Has the filter value been locked?
         */
        isFilterValueLocked: function( index ) {
            if ( index === 0 ) {
                return this.layerSpec.preserveRangeMin;
            }
            return this.layerSpec.preserveRangeMax;
        },


        /**
         * Set whether or not the filter value is locked
         */
        setFilterValueLocking: function( index, value ) {
            if ( index === 0 ) {
                this.layerSpec.preserveRangeMin = value;
            }
            this.layerSpec.preserveRangeMax = value;
        },


        /**
         * @param {number} zIndex - The new z-order value of the layer, where 0 is front.
         */
        setZIndex: function ( zIndex ) {
            // we by-pass the OpenLayers.Map.setLayerIndex() method and manually
            // set the z-index of the layer dev. setLayerIndex sets a relative
            // index based on current map layers, which then sets a z-index. This
            // caused issues with async layer loading.
            $( this.layer.olLayer_.div ).css( 'z-index', zIndex );
            PubSub.publish( this.getChannel(), { field: 'zIndex', value: zIndex });
        },


        /**
         * Get the layers zIndex
         */
        getZIndex: function () {
            return $( this.layer.olLayer_.div ).css( 'z-index' );
        },


        /**
         * Set the layers transformer type
         */
        setTransformerType: function ( transformerType ) {
            this.layerSpec.tileTransform.type = transformerType;
            this.update();
            PubSub.publish( this.getChannel(), { field: 'transformerType', value: transformerType });
        },


        /**
         * Get the layers transformer type
         */
        getTransformerType: function () {
            return this.layerSpec.tileTransform.type;
        },


        /**
         * Set the transformer data arguments
         */
        setTransformerData: function ( transformerData ) {
            this.layerSpec.tileTransform.data = transformerData;
            this.update();
            PubSub.publish( this.getChannel(), { field: 'transformerData', value: transformerData });
        },


        /**
         * Get the transformer data arguments
         */
        getTransformerData: function () {
            return this.layerSpec.tileTransform.data;
        },


        /**
         * Set the layer coarseness
         */
        setCoarseness: function( coarseness ) {
            this.layerSpec.renderer.coarseness = coarseness;
            this.setRampMinMax( getLevelMinMax( this ) );
            this.update();
            PubSub.publish( this.getChannel(), { field: 'coarseness', value: coarseness });
        },


        /**
         * Get the layer coarseness
         */
        getCoarseness: function() {
            return this.layerSpec.renderer.coarseness;
        },


        /**
         * Generate query parameters based on state of layer
         */
        generateQueryParamString: function() {
            var viewBounds = this.map.getTileBoundsInView(),
                query = {
                    minX: viewBounds.minX,
                    maxX: viewBounds.maxX,
                    minY: viewBounds.minY,
                    maxY: viewBounds.maxY,
                    minZ: viewBounds.minZ,
                    maxZ: viewBounds.maxZ,
                    renderer: this.layerSpec.renderer,
                    tileTransform: this.layerSpec.tileTransform,
                    valueTransform: this.layerSpec.valueTransform,
                    theme: this.layerSpec.theme
                };
            return '?'+encodeURIComponent( JSON.stringify( query ) );
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
                    fullUrl;

                if (x >= 0 && y >= 0) {
                    // set base url
                    fullUrl = (this.url + this.version + "/" +
                               this.layername + "/" +
                               z + "/" + x + "/" + y + "." + this.type);
                    return fullUrl + that.generateQueryParamString();
                }
            }

            if ( !this.map ) {
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
                        'url': this.layerSpec.source.tms,
                        'options': {
                            'layername': this.layerSpec.source.id,
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
            }

            // redraw this layer
            this.layer.olLayer_.redraw();
        }
    });

    return ServerLayer;
});
