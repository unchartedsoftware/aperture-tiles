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
        LayerUtil = require('./LayerUtil'),
        HtmlTileLayer = require('./HtmlTileLayer'),
        PubSub = require('../util/PubSub');

    function ClientLayer( spec ) {
        // set reasonable defaults
        spec.enabled = ( spec.enabled !== undefined ) ? spec.enabled : true;
        spec.opacity = ( spec.opacity !== undefined ) ? spec.opacity : 1.0;
        spec.zIndex = ( spec.zIndex !== undefined ) ? spec.zIndex : 1000;
        spec.domain = "client";
        // call base constructor    
        Layer.call( this, spec );
    }

    ClientLayer.prototype = Object.create( Layer.prototype );

    ClientLayer.prototype.activate = function() {

        // add the new layer
        this.olLayer = new HtmlTileLayer(
            'Client Rendered Tile Layer',
            this.spec.source.tms,
            {
                layername: this.spec.source.id,
                type: 'json',
                maxExtent: new OpenLayers.Bounds(-20037500, -20037500,
                    20037500,  20037500),
                isBaseLayer: false,
                getURL: LayerUtil.getURL,
                html: this.spec.html,
                renderer: this.spec.renderer,
                entry: this.spec.entry
            });

        this.map.olMap.addLayer( this.olLayer );

        this.setZIndex( this.spec.zIndex );
        this.setOpacity( this.spec.opacity );
        this.setVisibility( this.spec.enabled );
        this.setTheme( this.map.getTheme() );

        if ( this.spec.renderer ) {
            this.spec.renderer.meta = this.spec.source.meta.meta;
            this.spec.renderer.map = this.map;
            this.spec.renderer.parent = this;
        }
    };

    ClientLayer.prototype.deactivate = function() {
        this.map.olMap.removeLayer( this.olLayer );
        this.olLayer.destroy();
    };

    /**
     * Updates the theme associated with the layer
     */
    ClientLayer.prototype.setTheme = function( theme ) {
        this.spec.theme = theme;
    };

    /**
     * Get the current theme for the layer
     */
    ClientLayer.prototype.getTheme = function() {
        return this.spec.theme;
    };

    /**
     * @param zIndex {number} The new z-order value of the layer, where 0 is front.
     */
    ClientLayer.prototype.setZIndex = function ( zIndex ) {
        // we by-pass the OpenLayers.Map.setLayerIndex() method and manually
        // set the z-index of the layer dev. setLayerIndex sets a relative
        // index based on current map layers, which then sets a z-index. This
        // caused issues with async layer loading.
        this.spec.zIndex = zIndex;
        $( this.olLayer.div ).css( 'z-index', zIndex );
        PubSub.publish( this.getChannel(), { field: 'zIndex', value: zIndex });
    };

    /**
     * Get the layers zIndex
     */
    ClientLayer.prototype.getZIndex = function () {
        return this.spec.zIndex;
    };

    return ClientLayer;
});
