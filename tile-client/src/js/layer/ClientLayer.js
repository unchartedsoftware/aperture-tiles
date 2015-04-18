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

    var Layer = require('./Layer'),
        LayerUtil = require('./LayerUtil'),
        HtmlTileLayer = require('./HtmlTileLayer'),
        PubSub = require('../util/PubSub');

    /**
     * Instantiate a ClientLayer object.
     * @class ClientLayer
     * @augments Layer
     * @classdesc A client rendered layer object. Uses JSON data retrieved from the
     *            server in conjunction with a Renderer object or html function to
     *            create interactable DOM elements.
     *
     * @param {Object} spec - The specification object.
     * <pre>
     * {
     *     opacity  {float}    - The opacity of the layer. Default = 1.0
     *     enabled  {boolean}  - Whether the layer is visible or not. Default = true
     *     zIndex   {integer}  - The z index of the layer. Default = 1000
     *     renderer {Renderer} - The tile renderer object. (optional)
     *     html {String|Function|HTMLElement|jQuery} - The html for the tile. (optional)
     * }
     * </pre>
     */
    function ClientLayer( spec ) {
        // call base constructor
        Layer.call( this, spec );
        // set reasonable defaults
        this.zIndex = ( spec.zIndex !== undefined ) ? spec.zIndex : 1000;
        this.domain = "client";
        this.source = spec.source;
        this.getURL = spec.getURL || LayerUtil.getURL;
        if ( spec.tileClass ) {
            this.tileClass = spec.tileClass;
        }
        if ( spec.renderer ) {
            this.setRenderer( spec.renderer );
        }
        if ( spec.html ) {
            this.html = spec.html;
        }
    }

    ClientLayer.prototype = Object.create( Layer.prototype );

    /**
     * Activates the layer object. This should never be called manually.
     * @memberof ClientLayer
     * @private
     */
    ClientLayer.prototype.activate = function() {

        // add the new layer
        this.olLayer = new HtmlTileLayer(
            'Client Rendered Tile Layer',
            this.source.tms,
            {
                layername: this.source.id,
                type: 'json',
                maxExtent: new OpenLayers.Bounds(-20037500, -20037500,
                    20037500,  20037500),
                isBaseLayer: false,
                getURL: this.getURL,
                tileClass: this.tileClass,
                html: this.html,
                renderer: this.renderer
            });

        this.map.olMap.addLayer( this.olLayer );

        this.setZIndex( this.zIndex );
        this.setOpacity( this.opacity );
        this.setEnabled( this.enabled );
        this.setTheme( this.map.getTheme() );

        PubSub.publish( this.getChannel(), { field: 'activate', value: true } );
    };

    /**
     * Dectivates the layer object. This should never be called manually.
     * @memberof ClientLayer
     * @private
     */
    ClientLayer.prototype.deactivate = function() {
        if ( this.olLayer ) {
            this.map.olMap.removeLayer( this.olLayer );
            this.olLayer.destroy();
            this.olLayer = null;
        }
        PubSub.publish( this.getChannel(), { field: 'deactivate', value: true } );
    };

    /**
     * Sets the current renderer of the layer.
     * @memberof ClientLayer
     *
     * @param {Renderer} renderer - The renderer to attach to the layer.
     */
    ClientLayer.prototype.setRenderer = function( renderer ) {
        this.renderer = renderer;
        this.renderer.attach( this );
    };

    /**
     * Updates the theme associated with the layer.
     * @memberof ClientLayer
     *
     * @param {String} theme - The theme identifier string.
     */
    ClientLayer.prototype.setTheme = function( theme ) {
        this.theme = theme;
    };

    /**
     * Get the current theme for the layer.
     * @memberof ClientLayer
     *
     * @returns {String} The theme identifier string.
     */
    ClientLayer.prototype.getTheme = function() {
        return this.theme;
    };

    /**
     * Set the z index of the layer.
     * @memberof ClientLayer
     *
     * @param {integer} zIndex - The new z-order value of the layer, where 0 is front.
     */
    ClientLayer.prototype.setZIndex = function ( zIndex ) {
        // we by-pass the OpenLayers.Map.setLayerIndex() method and manually
        // set the z-index of the layer dev. setLayerIndex sets a relative
        // index based on current map layers, which then sets a z-index. This
        // caused issues with async layer loading.
        this.zIndex = zIndex;
        if ( this.olLayer ) {
            $( this.olLayer.div ).css( 'z-index', zIndex );
        }
        PubSub.publish( this.getChannel(), { field: 'zIndex', value: zIndex });
    };

    /**
     * Get the layers zIndex.
     * @memberof ClientLayer
     *
     * @returns {integer} The zIndex for the layer.
     */
    ClientLayer.prototype.getZIndex = function () {
        return this.zIndex;
    };

    /**
     * Set the layers tile transform function type.
     * @memberof ClientLayer
     *
     * @param {String} transformType - The tile transformer type.
     */
    ClientLayer.prototype.setTileTransformType = function ( transformType ) {
        if ( this.tileTransform.type !== transformType ) {
            this.tileTransform.type = transformType;
            if ( this.olLayer ) {
                this.olLayer.redraw();
            }
            PubSub.publish( this.getChannel(), {field: 'tileTransformType', value: transformType} );
        }
    };

    /**
     * Get the layers transformer type.
     * @memberof ClientLayer
     *
     * @return {String} The tile transform type.
     */
    ClientLayer.prototype.getTileTransformType = function () {
        return this.tileTransform.type;
    };

    /**
     * Set the tile transform data attribute
     * @memberof ClientLayer
     *
     * @param {Object} transformData - The tile transform data attribute.
     */
    ClientLayer.prototype.setTileTransformData = function ( transformData ) {
        if ( this.tileTransform.data !== transformData ) {
            this.tileTransform.data = transformData;
            if ( this.olLayer ) {
                this.olLayer.redraw();
            }
            PubSub.publish( this.getChannel(), {field: 'tileTransformData', value: transformData} );
        }
    };

    /**
     * Get the transformer data attribute.
     * @memberof ClientLayer
     *
     * @returns {Object} The tile transform data attribute.
     */
    ClientLayer.prototype.getTileTransformData = function () {
        return this.tileTransform.data;
    };

    /**
     * Redraws the entire layer.
     * @memberof ClientLayer
     */
    ClientLayer.prototype.redraw = function () {
        if ( this.olLayer ) {
             this.olLayer.redraw();
        }
    };

    module.exports = ClientLayer;
}());
