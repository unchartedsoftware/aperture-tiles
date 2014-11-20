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

    var Layer = require('../Layer'),
        PubSub = require('../../util/PubSub'),
        requestRampImage,
        getLevelMinMax;

    /**
     * Request colour ramp image from server.
     *
     * @param {Object} layer - The layer object.
     * @param {Object} level - The current map zoom level.
     */
    requestRampImage = function( layer ) {
        function generateQueryParamString() {
            var query = {
                    renderer: layer.spec.renderer,
                    theme: layer.spec.theme
                };
            return '?'+encodeURIComponent( JSON.stringify( query ) );
        }
        aperture.io.rest('/v1.0/legend/' + layer.spec.source.id + generateQueryParamString(),
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
            coarseness = layer.spec.renderer.coarseness,
            adjustedZoom = zoomLevel - (coarseness-1),
            meta =  layer.spec.source.meta.meta,
            minArray = (meta && (meta.levelMinFreq || meta.levelMinimums || meta[adjustedZoom])),
            maxArray = (meta && (meta.levelMaxFreq || meta.levelMaximums || meta[adjustedZoom])),
            min = minArray ? ($.isArray(minArray) ? minArray[adjustedZoom] : minArray.minimum) : 0,
            max = maxArray ? ($.isArray(maxArray) ? maxArray[adjustedZoom] : maxArray.maximum) : 0;
        return [ parseFloat(min), parseFloat(max) ];
    };

    function ServerLayer( spec ) {
        // set reasonable defaults
        spec.zIndex = ( spec.zIndex !== undefined ) ? spec.zIndex : 1;
        spec.opacity = ( spec.opacity !== undefined ) ? spec.opacity : 1.0;
        spec.enabled = ( spec.enabled !== undefined ) ? spec.enabled : true;
        spec.renderer = spec.renderer || {};
        spec.renderer.coarseness = ( spec.renderer.coarseness !== undefined ) ?  spec.renderer.coarseness : 1;
        spec.renderer.ramp = spec.ramp || "spectral";
        spec.renderer.rangeMin = ( spec.renderer.rangeMin !== undefined ) ? spec.renderer.rangeMin : 0;
        spec.renderer.rangeMax = ( spec.renderer.rangeMax !== undefined ) ? spec.renderer.rangeMax : 100;
        spec.renderer.preserveRangeMin = spec.preserveRangeMin || false;
        spec.renderer.preserveRangeMax = spec.preserveRangeMax || false;
        spec.valueTransform = spec.valueTransform || { type: 'linear' };
        spec.tileTransform = spec.tileTransform || { type: 'identity' };
        spec.domain = "server";
        // call base constructor
        Layer.call( this, spec );
    }

    ServerLayer.prototype = Object.create( Layer.prototype );

    ServerLayer.prototype.activate = function() {
        var that = this;
        // set callback to update ramp min/max on zoom
        this.map.on("zoomend", function() {
            if ( that.layer ) {
                that.setRampMinMax( getLevelMinMax( that ) );
            }
        });
        this.update();
        requestRampImage( this );
        this.setZIndex( this.spec.zIndex );
        this.setOpacity( this.spec.opacity );
        this.setVisibility( this.spec.enabled );
        this.setTheme( this.map.getTheme() );
        this.setRampMinMax( getLevelMinMax( that ) );
    };

    ServerLayer.prototype.deactivate = function() {
        // TODO: implement
        return true;
    };

    /**
     * Valid ramp type strings.
     */
    ServerLayer.prototype.RAMP_TYPES = [
         {id: "spectral", name: "Spectral"},
         {id: "hot", name: "Hot"},
         {id: "polar", name: "Polar"},
         {id: "neutral", name: "Neutral"},
         {id: "cool", name: "Cool"},
         {id: "valence", name: "Valence"},
         {id: "flat", name: "Flat"}
    ];

    /**
     * Valid ramp function strings.
     */
    ServerLayer.prototype.RAMP_FUNCTIONS = [
        {id: "linear", name: "Linear"},
        {id: "log10", name: "Log 10"}
    ];

    /**
     * Set the opacity
     */
    ServerLayer.prototype.setOpacity = function ( opacity ) {
        if (this.layer) {
            this.layer.olLayer_.setOpacity( opacity );
            PubSub.publish( this.getChannel(), { field: 'opacity', value: opacity });
        }
    };

    /**
     *  Get layer opacity
     */
    ServerLayer.prototype.getOpacity = function() {
        return this.layer.olLayer_.opacity;
    };

    /**
     * Set the visibility
     */
    ServerLayer.prototype.setVisibility = function ( visibility ) {
        if (this.layer) {
            this.layer.olLayer_.setVisibility( visibility );
            PubSub.publish( this.getChannel(), { field: 'visibility', value: visibility });
        }
    };

    /**
     * Get layer visibility
     */
    ServerLayer.prototype.getVisibility = function() {
        return this.layer.olLayer_.getVisibility();
    };

    /**
     * Updates the ramp type associated with the layer
     */
    ServerLayer.prototype.setRampType = function ( rampType ) {
        var that = this;
        this.spec.renderer.ramp = rampType;
        requestRampImage( that );
        this.update();
    };

    /**
     *  Get ramp type for layer
     */
    ServerLayer.prototype.getRampType = function() {
        return this.spec.renderer.ramp;
    };

    /**
     * Updates the theme associated with the layer
     */
    ServerLayer.prototype.setTheme = function () {
        var that = this;
        this.spec.renderer.theme = this.map.getTheme();
        requestRampImage( that );
        this.update();
        this.setZIndex( this.getZIndex() ); // update z index, since changing baselayer resets them
    };

    /**
     * Get the current theme for the layer
     */
    ServerLayer.prototype.getTheme = function() {
        return this.spec.renderer.theme;
    };

    /**
     * Sets the ramps current min and max
     */
    ServerLayer.prototype.setRampMinMax = function( minMax ) {
        this.rampMinMax = minMax;
        PubSub.publish( this.getChannel(), { field: 'rampMinMax', value: minMax });
    };

    /**
     * Get the ramps current min and max for the zoom level
     */
    ServerLayer.prototype.getRampMinMax = function() {
        return this.rampMinMax;
    };

    /**
     * Update the ramps URL string
     */
    ServerLayer.prototype.setRampImageUrl = function ( url ) {
        this.rampImageUrl = url;
        PubSub.publish( this.getChannel(), { field: 'rampImageUrl', value: url });
    };

    /**
     * Get the current ramps URL string
     */
    ServerLayer.prototype.getRampImageUrl = function() {
        return this.rampImageUrl;
    };

    /**
     * Updates the ramp function associated with the layer.  Results in a POST
     * to the server.
     *
     * @param {string} rampFunction - The new new ramp function.
     */
    ServerLayer.prototype.setRampFunction = function ( rampFunction ) {
        this.spec.valueTransform = { type: rampFunction };
        this.update();
        PubSub.publish( this.getChannel(), { field: 'rampFunction', value: rampFunction });
    };

    /**
     * Get the current ramps function
     */
    ServerLayer.prototype.getRampFunction = function() {
        return this.spec.valueTransform.type;
    };

    /**
     * Updates the filter range for the layer.  Results in a POST to the server.
     *
     * @param {Array} filterRange - A two element array with values in the range [0.0, 1.0],
     * where the first element is the min range, and the second is the max range.
     */
    ServerLayer.prototype.setFilterRange = function ( min, max ) {
        this.spec.renderer.rangeMin = min;
        this.spec.renderer.rangeMax = max;
        this.update();
        PubSub.publish( this.getChannel(), { field: 'filterRange', value: [ min, max ] });
    };

    /**
     * Get the current ramp filter range from 0 to 100
     */
    ServerLayer.prototype.getFilterRange = function() {
        return [ this.spec.renderer.rangeMin, this.spec.renderer.rangeMax ];
    };

    /**
     * Updates the filter values for the layer.  Results in a POST to the server.
     *
     * @param {Array} filterRange - A two element array with values in the range levelMin and levelMax,
     * where the first element is the min range, and the second is the max range.
     */
    ServerLayer.prototype.setFilterValues = function ( min, max ) {
        this.filterValues = [ min, max ];
        PubSub.publish( this.getChannel(), { field: 'filterValues', value: [ min, max ] });
    };

    /**
     * Get the current ramp filter values from levelMin and levelMax
     */
    ServerLayer.prototype.getFilterValues = function() {
        return this.filterValues || this.getRampMinMax();
    };

    /**
     * Has the filter value been locked?
     */
    ServerLayer.prototype.isFilterValueLocked = function( index ) {
        if ( index === 0 ) {
            return this.spec.preserveRangeMin;
        }
        return this.spec.preserveRangeMax;
    };

    /**
     * Set whether or not the filter value is locked
     */
    ServerLayer.prototype.setFilterValueLocking = function( index, value ) {
        if ( index === 0 ) {
            this.spec.preserveRangeMin = value;
        }
        this.spec.preserveRangeMax = value;
    };

    /**
     * @param {number} zIndex - The new z-order value of the layer, where 0 is front.
     */
    ServerLayer.prototype.setZIndex = function ( zIndex ) {
        // we by-pass the OpenLayers.Map.setLayerIndex() method and manually
        // set the z-index of the layer dev. setLayerIndex sets a relative
        // index based on current map layers, which then sets a z-index. This
        // caused issues with async layer loading.
        this.spec.zIndex = zIndex;
        $( this.layer.olLayer_.div ).css( 'z-index', zIndex );
        PubSub.publish( this.getChannel(), { field: 'zIndex', value: zIndex });
    };

    /**
     * Get the layers zIndex
     */
    ServerLayer.prototype.getZIndex = function () {
        return this.spec.zIndex;
    };

    /**
     * Set the layers transformer type
     */
    ServerLayer.prototype.setTransformerType = function ( transformerType ) {
        this.spec.tileTransform.type = transformerType;
        this.update();
        PubSub.publish( this.getChannel(), { field: 'transformerType', value: transformerType });
    };

    /**
     * Get the layers transformer type
     */
    ServerLayer.prototype.getTransformerType = function () {
        return this.spec.tileTransform.type;
    };

    /**
     * Set the transformer data arguments
     */
    ServerLayer.prototype.setTransformerData = function ( transformerData ) {
        this.spec.tileTransform.data = transformerData;
        this.update();
        PubSub.publish( this.getChannel(), { field: 'transformerData', value: transformerData });
    };

    /**
     * Get the transformer data arguments
     */
    ServerLayer.prototype.getTransformerData = function () {
        return this.spec.tileTransform.data;
    };

    /**
     * Set the layer coarseness
     */
    ServerLayer.prototype.setCoarseness = function( coarseness ) {
        this.spec.renderer.coarseness = coarseness;
        this.setRampMinMax( getLevelMinMax( this ) );
        this.update();
        PubSub.publish( this.getChannel(), { field: 'coarseness', value: coarseness });
    };

    /**
     * Get the layer coarseness
     */
    ServerLayer.prototype.getCoarseness = function() {
        return this.spec.renderer.coarseness;
    };

    /**
     * Generate query parameters based on state of layer
     */
    ServerLayer.prototype.generateQueryParamString = function() {
        var viewBounds = this.map.getTileBoundsInView(),
            query = {
                minX: viewBounds.minX,
                maxX: viewBounds.maxX,
                minY: viewBounds.minY,
                maxY: viewBounds.maxY,
                minZ: viewBounds.minZ,
                maxZ: viewBounds.maxZ,
                renderer: this.spec.renderer,
                tileTransform: this.spec.tileTransform,
                valueTransform: this.spec.valueTransform,
                theme: this.spec.theme
            };
        return '?'+encodeURIComponent( JSON.stringify( query ) );
    };

    /**
     * Update all our openlayers layers on our map.
     */
    ServerLayer.prototype.update = function () {

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
                fullUrl = ( this.url + this.layername + "/" +
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
        yFunction = ( this.spec.isDensityStrip ) ? clampY : passY;

        if ( !this.layer ) {

            // add the new layer
            this.layer = this.map.addApertureLayer(
                aperture.geo.MapTileLayer.TMS, {},
                {
                    'name': 'Aperture Tile Layers',
                    'url': this.spec.source.tms,
                    'options': {
                        'layername': this.spec.source.id,
                        'type': 'png',
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
    };

    return ServerLayer;
});
