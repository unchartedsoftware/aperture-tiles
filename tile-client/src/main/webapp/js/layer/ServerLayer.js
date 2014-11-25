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

    var Layer = require('./Layer'),
        PubSub = require('../util/PubSub'),
        requestRampImage,
        getLevelMinMax;

    /**
     * Request colour ramp image from server.
     *
     * @param {Object} layer - The layer object.
     */
    requestRampImage = function( layer ) {
        function generateQueryParamString() {
            var query = {
                    renderer: layer.spec.renderer,
                    theme: layer.spec.theme
                };
            return '?'+encodeURIComponent( JSON.stringify( query ) );
        }
        $.get('rest/v1.0/legend/' + layer.spec.source.id + generateQueryParamString(),
             'GET',
             function ( legendString ) {
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
            meta =  layer.spec.source.meta,
            levelMinMax = meta.minMax[ adjustedZoom ],
            minMax = levelMinMax ? levelMinMax.minMax : {
                min: null,
                max: null
            };
        return [ minMax.min, minMax.max ];
    };

    function ServerLayer( spec ) {
        // set reasonable defaults
        spec.zIndex = ( spec.zIndex !== undefined ) ? spec.zIndex : 1;

        spec.renderer = spec.renderer || {};
        spec.renderer.coarseness = ( spec.renderer.coarseness !== undefined ) ?  spec.renderer.coarseness : 1;
        spec.renderer.ramp = spec.renderer.ramp || "spectral";
        spec.renderer.rangeMin = ( spec.renderer.rangeMin !== undefined ) ? spec.renderer.rangeMin : 0;
        spec.renderer.rangeMax = ( spec.renderer.rangeMax !== undefined ) ? spec.renderer.rangeMax : 100;
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
                that.setLevelMinMax( getLevelMinMax( that ) );
            }
        });

        function getURL( bounds ) {
            var res = this.map.getResolution(),
                maxBounds = this.maxExtent,
                tileSize = this.tileSize,
                x = Math.round( (bounds.left-maxBounds.left) / (res*tileSize.w) ),
                y = Math.round( (bounds.bottom-maxBounds.bottom) / (res*tileSize.h) ),
                z = this.map.getZoom();
            if ( x >= 0 && y >= 0 ) {
                return this.url + this.layername
                    + "/" + z + "/" + x + "/" + y + "."
                    + this.type + that.getQueryParamString();
            }
        }

        // add the new layer
        this.layer = new OpenLayers.Layer.TMS(
            'Aperture Tile Layers',
            this.spec.source.tms,
            {
                layername: this.spec.source.id,
                type: 'png',
                maxExtent: new OpenLayers.Bounds(-20037500, -20037500,
                                                 20037500,  20037500),
                transparent: true,
                isBaseLayer: false,
                getURL: getURL
            });
        this.map.map.addLayer( this.layer );

        requestRampImage( this );
        this.setZIndex( this.spec.zIndex );
        this.setOpacity( this.spec.opacity );
        this.setVisibility( this.spec.enabled );
        this.setTheme( this.map.getTheme() );
        this.setLevelMinMax( getLevelMinMax( that ) );
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
     * @param {number} zIndex - The new z-order value of the layer, where 0 is front.
     */
    ServerLayer.prototype.setZIndex = function ( zIndex ) {
        // we by-pass the OpenLayers.Map.setLayerIndex() method and manually
        // set the z-index of the layer dev. setLayerIndex sets a relative
        // index based on current map layers, which then sets a z-index. This
        // caused issues with async layer loading.
        this.spec.zIndex = zIndex;
        if ( this.layer ) {
            $( this.layer.div ).css( 'z-index', zIndex );
            PubSub.publish( this.getChannel(), { field: 'zIndex', value: zIndex });
        }
    };

    /**
     * Get the layers zIndex
     */
    ServerLayer.prototype.getZIndex = function () {
        return this.spec.zIndex;
    };

    /**
     * Updates the ramp type associated with the layer
     */
    ServerLayer.prototype.setRampType = function ( rampType ) {
        var that = this;
        this.spec.renderer.ramp = rampType;
        requestRampImage( that );
        this.layer.redraw();
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
    ServerLayer.prototype.setTheme = function( theme ) {
        var that = this;
        this.spec.renderer.theme = theme;
        requestRampImage( that );
        this.layer.redraw();
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
    ServerLayer.prototype.setLevelMinMax = function( minMax ) {
        this.levelMinMax = minMax;
        PubSub.publish( this.getChannel(), { field: 'levelMinMax', value: minMax });
    };

    /**
     * Get the ramps current min and max for the zoom level
     */
    ServerLayer.prototype.getLevelMinMax = function() {
        return this.levelMinMax;
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
        this.layer.redraw();
        PubSub.publish( this.getChannel(), { field: 'rampFunction', value: rampFunction });
    };

    /**
     * Get the current ramps function
     */
    ServerLayer.prototype.getRampFunction = function() {
        return this.spec.valueTransform.type;
    };

    /**
     * Set the layers transformer type
     */
    ServerLayer.prototype.setTransformerType = function ( transformerType ) {
        this.spec.tileTransform.type = transformerType;
        this.layer.redraw();
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
        this.layer.redraw();
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
        this.setLevelMinMax( getLevelMinMax( this ) );
        this.layer.redraw();
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
    ServerLayer.prototype.getQueryParamString = function() {
        var query = {
                renderer: this.spec.renderer,
                tileTransform: this.spec.tileTransform,
                valueTransform: this.spec.valueTransform,
                theme: this.spec.theme
            };
        return '?'+encodeURIComponent( JSON.stringify( query ) );
    };

    return ServerLayer;
});
