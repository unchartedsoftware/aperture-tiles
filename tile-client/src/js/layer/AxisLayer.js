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
        UnivariateTileLayer = require('./UnivariateTileLayer'),
        PubSub = require('../util/PubSub');

    /**
     * Instantiate a AxisLayer object.
     * @class AxisLayer
     * @augments Layer
     * @classdesc A axis rendered layer object. Uses data received from the server and
     *            renders it over the axis.
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
    function AxisLayer( spec ) {
        // call base constructor
        Layer.call( this, spec );
        // set reasonable defaults
        this.zIndex = ( spec.zIndex !== undefined ) ? spec.zIndex : 1500;
        this.domain = "axis";
        this.source = spec.source;
        this.getURL = spec.getURL || LayerUtil.getURL;
        this.dimension = spec.dimension || 'x';
        if ( spec.tileClass) {
            this.tileClass = spec.tileClass;
        }
        if ( spec.renderer ) {
            this.renderer = spec.renderer;
        }
        if ( spec.html ) {
            this.html = spec.html;
        }
    }

    AxisLayer.prototype = Object.create( Layer.prototype );

    /**
     * Activates the layer object. This should never be called manually.
     * @memberof AxisLayer
     * @private
     */
    AxisLayer.prototype.activate = function() {
        // add the new layer
        this.olLayer = new UnivariateTileLayer(
            'Axis Rendered Tile Layer',
            this.source.tms,
            {
                layername: this.source.id,
                type: 'json',
                maxExtent: new OpenLayers.Bounds(-20037500, -20037500,
                    20037500,  20037500),
                isBaseLayer: false,
                getURL: this.getURL,
                html: this.html,
                tileClass: this.tileClass,
                renderer: this.renderer,
                dimension: this.dimension
            });

        this.map.olMap.addLayer( this.olLayer );

        this.setZIndex( this.zIndex );
        this.setOpacity( this.opacity );
        this.setEnabled( this.enabled );
        this.setTheme( this.map.getTheme() );

        if ( this.renderer ) {
            this.renderer.attach( this );
        }
        PubSub.publish( this.getChannel(), { field: 'activate', value: true } );
    };

    /**
     * Dectivates the layer object. This should never be called manually.
     * @memberof AxisLayer
     * @private
     */
    AxisLayer.prototype.deactivate = function() {
        if ( this.olLayer ) {
            this.map.olMap.removeLayer( this.olLayer );
            this.olLayer.destroy();
            this.olLayer = null;
        }
        PubSub.publish( this.getChannel(), { field: 'deactivate', value: true } );
    };

    /**
     * Updates the theme associated with the layer.
     * @memberof AxisLayer
     *
     * @param {String} theme - The theme identifier string.
     */
    AxisLayer.prototype.setTheme = function( theme ) {
        this.theme = theme;
    };

    /**
     * Get the current theme for the layer.
     * @memberof AxisLayer
     *
     * @returns {String} The theme identifier string.
     */
    AxisLayer.prototype.getTheme = function() {
        return this.theme;
    };

    /**
     * Set the z index of the layer.
     * @memberof AxisLayer
     *
     * @param {integer} zIndex - The new z-order value of the layer, where 0 is front.
     */
    AxisLayer.prototype.setZIndex = function ( zIndex ) {
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
     * @memberof AxisLayer
     *
     * @returns {integer} The zIndex for the layer.
     */
    AxisLayer.prototype.getZIndex = function () {
        return this.zIndex;
    };

    /**
     * Redraws the entire layer.
     * @memberof AxisLayer
     */
    AxisLayer.prototype.redraw = function () {
        if ( this.olLayer ) {
             this.olLayer.redraw();
        }
    };

    module.exports = AxisLayer;
}());
