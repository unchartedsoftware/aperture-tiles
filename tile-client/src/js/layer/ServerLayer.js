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


/**
 * A server rendered image layer that displays images retrieved from the server.
 * Respective server side rendering parameters may be modified using the interface
 * of the object.
 */
( function() {

    "use strict";

    var Util = require('../util/Util'),
        Layer = require('./Layer'),
        LayerUtil = require('./LayerUtil'),
        PubSub = require('../util/PubSub'),
        LegendService = require('../rest/LegendService'),
        setRampImageUrl,
        setLevelMinMax,
        zoomCallback;

    /**
     * Private: Request colour ramp image from server and set layer property when received.
     *
     * @param layer {Object} the layer object
     */
    setRampImageUrl = function( layer ) {
        LegendService.getEncodedImage( layer.spec.source.id, {
                renderer: layer.spec.renderer
            }, function ( url ) {
                layer.rampImageUrl = url;
                PubSub.publish( layer.getChannel(), { field: 'rampImageUrl', value: url });
            });
    };

    /**
     * Private: Sets the layers min and max values for the given zoom level.
     *
     * @param layer {Object} the layer object.
     */
    setLevelMinMax = function( layer ) {
        var zoomLevel = layer.map.getZoom(),
            coarseness = layer.spec.renderer.coarseness,
            adjustedZoom = zoomLevel - ( coarseness-1 ),
            meta =  layer.spec.source.meta,
            levelMinMax = meta.minMax[ adjustedZoom ],
            minMax = levelMinMax ? levelMinMax.minMax : {
                min: null,
                max: null
            };
        layer.levelMinMax =  [ minMax.min, minMax.max ];
        PubSub.publish( layer.getChannel(), { field: 'levelMinMax', value: minMax });
    };

    /**
     * Private: Returns the zoom callback function to update level min and maxes.
     *
     * @param layer {ServerLayer} The layer object.
     */
    zoomCallback = function( layer ) {
        return function() {
            if ( layer.olLayer ) {
                setLevelMinMax( layer );
            }
        };
    };

    /**
     * Instantiate a ServerLayer object.
     *
     * @param spec {Object} The Specification object.
     * {
     *     opacity {float}   The opacity of the layer. Default = 1.0
     *     enabled {boolean} Whether the layer is visible or not. Default = true
     *     zIndex  {integer} The z index of the layer. Default = 1
     *     renderer: {
     *         coarseness {integer} The pixel by pixel coarseness. Default based on server configuration.
     *         ramp       {String}  The color ramp type. Default based on server configuration.
     *         rangeMin   {integer} The minimum percentage to clamp the low end of the color ramp. Default based on
     *                                server configuration.
     *         rangeMax   {integer} The maximum percentage to clamp the high end of the color ramp. Default based on
     *                              server configuration.
     *     },
     *     valueTransform: {
     *         type {String} Value transformer type. Default based on server configuration.
     *     },
     *     tileTransform: {
     *         type {String} Tile transformer type. Default based on server configuration.
     *         data {Object} The tile transformer data initialization object. Default based on server configuration.
     *     }
     * }
     */
    function ServerLayer( spec ) {
        // set reasonable defaults
        spec.zIndex = ( spec.zIndex !== undefined ) ? spec.zIndex : 1;
        spec.renderer = spec.renderer || {};
        spec.valueTransform = spec.valueTransform || {};
        spec.tileTransform = spec.tileTransform || {};
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

        this.setZIndex( this.spec.zIndex );
        this.setOpacity( this.spec.opacity );
        this.setVisibility( this.spec.enabled );
        this.setTheme( this.map.getTheme() ); // sends initial request for ramp image
        setLevelMinMax( this );
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
     * Set the z index of the layer.
     *
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
     * Get the layers z index.
     */
    ServerLayer.prototype.getZIndex = function () {
        return this.spec.zIndex;
    };

    /**
     * Set the ramp type associated with the layer.
     *
     * @param rampType {String} The ramp type used to render the images.
     */
    ServerLayer.prototype.setRampType = function ( rampType ) {
        var that = this;
        this.spec.renderer.ramp = rampType;
        setRampImageUrl( that );
        this.olLayer.redraw();
    };

    /**
     *  Get ramp type for layer
     */
    ServerLayer.prototype.getRampType = function() {
        return this.spec.renderer.ramp;
    };

    /**
     * Set the theme associated with the layer.
     *
     * @param theme {String} The theme of the layer.
     */
    ServerLayer.prototype.setTheme = function( theme ) {
        var that = this;
        this.spec.renderer.theme = theme;
        setRampImageUrl( that );
        this.olLayer.redraw();
        this.setZIndex( this.getZIndex() ); // update z index, since changing baselayer resets them
    };

    /**
     * Get the current theme for the layer.
     */
    ServerLayer.prototype.getTheme = function() {
        return this.spec.renderer.theme;
    };

    /**
     * Get the current minimum and maximum values for the current zoom level.
     */
    ServerLayer.prototype.getLevelMinMax = function() {
        return this.levelMinMax;
    };

    /**
     * Get the current ramp image URL string
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
    ServerLayer.prototype.setValueTransformType = function ( rampFunction ) {
        this.spec.valueTransform = { type: rampFunction };
        this.olLayer.redraw();
        PubSub.publish( this.getChannel(), { field: 'valueTransformType', value: rampFunction });
    };

    /**
     * Get the current ramps function
     */
    ServerLayer.prototype.getValueTransformType = function() {
        return this.spec.valueTransform.type;
    };

    /**
     * Set the layers tile transform type
     *
     * @param transformType {String} The tile transformer type.
     */
    ServerLayer.prototype.setTileTransformType = function ( transformType ) {
        this.spec.tileTransform.type = transformType;
        this.olLayer.redraw();
        PubSub.publish( this.getChannel(), { field: 'tileTransformType', value: transformType });
    };

    /**
     * Get the layers transformer type
     */
    ServerLayer.prototype.getTileTransformType = function () {
        return this.spec.tileTransform.type;
    };

    /**
     * Set the tile transform data attribute
     *
     * @param transformData {Object} The tile transform data attribute.
     */
    ServerLayer.prototype.setTileTransformData = function ( transformData ) {
        this.spec.tileTransform.data = transformData;
        this.olLayer.redraw();
        PubSub.publish( this.getChannel(), { field: 'tileTransformData', value: transformData });
    };

    /**
     * Get the transformer data arguments
     */
    ServerLayer.prototype.getTileTransformData = function () {
        return this.spec.tileTransform.data;
    };

    /**
     * Set the layer coarseness.
     *
     * @param coarseness {int} The pixel by pixel coarseness of the layer
     */
    ServerLayer.prototype.setCoarseness = function( coarseness ) {
        this.spec.renderer.coarseness = coarseness;
        setLevelMinMax( this ); // coarseness modifies the min/max
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
        var query = {
            renderer: this.spec.renderer,
            tileTransform: this.spec.tileTransform,
            valueTransform: this.spec.valueTransform
        };
        return Util.encodeQueryParams( query );
    };

    module.exports = ServerLayer;
}());
