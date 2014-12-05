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

    var OpenLayers = require('../openlayers/OpenLayers.min'),
        $ = require('jquery'),
        _ = require('lodash'),
        Layer = require('./Layer'),
        LayerUtil = require('./LayerUtil'),
        PubSub = require('../util/PubSub'),
        LegendService = require('../rest/LegendService'),
        requestRampImage,
        getLevelMinMax,
        zoomCallback;

    /**
     * Private: Request colour ramp image from server.
     *
     * @param layer {Object} the layer object
     */
    requestRampImage = function( layer ) {
        LegendService.getEncodedImage( layer.spec.source.id, {
                renderer: layer.spec.renderer
            }, function ( legendString ) {
                 layer.setRampImageUrl( legendString );
            });
    };

    /**
     * Private: Returns the layers min and max values for the given zoom level.
     *
     * @param layer {Object} the layer object.
     */
    getLevelMinMax = function( layer ) {
        var zoomLevel = layer.map.getZoom(),
            coarseness = layer.spec.renderer.coarseness,
            adjustedZoom = zoomLevel - ( coarseness-1 ),
            meta =  layer.spec.source.meta,
            levelMinMax = meta.minMax[ adjustedZoom ],
            minMax = levelMinMax ? levelMinMax.minMax : {
                min: null,
                max: null
            };
        return [ minMax.min, minMax.max ];
    };

    /**
     * Private: Returns the zoom callback function to update level min and maxes.
     *
     * @param layer {ServerLayer} The layer object.
     */
    zoomCallback = function( layer ) {
        return function() {
            if ( layer.olLayer ) {
                layer.setLevelMinMax( getLevelMinMax( layer ) );
            }
        };
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
        // set callback here so it can be removed later
        this.zoomCallback = zoomCallback( this );
        // set callback to update ramp min/max on zoom
        this.map.on( "zoomend", this.zoomCallback );

        function getURL( bounds ) {
            return LayerUtil.getURL.call( this, bounds ) + that.getQueryParamString();
        }

        // add the new layer
        this.olLayer = new OpenLayers.Layer.TMS(
            'Server Rendered Tile Layer',
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
        this.map.olMap.addLayer( this.olLayer );

        requestRampImage( this );
        this.setZIndex( this.spec.zIndex );
        this.setOpacity( this.spec.opacity );
        this.setVisibility( this.spec.enabled );
        this.setTheme( this.map.getTheme() );
        this.setLevelMinMax( getLevelMinMax( that ) );
    };

    ServerLayer.prototype.deactivate = function() {
        if ( this.olLayer ) {
            this.map.olMap.removeLayer( this.olLayer );
            this.olLayer.destroy();
        }
        this.map.off( "zoomend", this.zoomCallback );
        this.zoomCallback = null;
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
        if ( this.olLayer ) {
            $( this.olLayer.div ).css( 'z-index', zIndex );
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
        this.olLayer.redraw();
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
        this.olLayer.redraw();
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
        this.olLayer.redraw();
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
        this.olLayer.redraw();
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
        this.olLayer.redraw();
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
        this.olLayer.redraw();
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
        function encodeQueryParams( params ) {
            var query;
            function traverseParams( params, query ) {
                var result = "";
                _.forIn( params, function( value, key ) {
                    if ( typeof value !== "object" ) {
                        result += query + key + '=' + value + "&";
                    } else {
                        result += traverseParams( params[ key ], query + key + "." );
                    }
                });
                return result;
            }
            query = "?" + traverseParams( params, '' );
            return query.slice( 0, query.length - 1 );
        }
        var query = {
                renderer: this.spec.renderer,
                tileTransform: this.spec.tileTransform,
                valueTransform: this.spec.valueTransform
            };

        return encodeQueryParams( query );
    };

    module.exports = ServerLayer;
}());
