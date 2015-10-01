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

    var Util = require('../util/Util'),
        PubSub = require('../util/PubSub');

    /**
     * Instantiate a Layer object.
     * @class Layer
     * @classdesc A Layer class, the base class for all layer implementations.
     *
     * @param {Object} spec - The specification object.
     */
    function Layer( spec ) {
        spec = spec || {};
        this.uuid = Util.generateUuid();
        this.name = spec.name || "Unnamed Layer";
        this.domain = spec.domain;
        this.map = spec.map;
        this.showPendingTiles = spec.showPendingTiles !== undefined ? spec.showPendingTiles : true;
        this.opacity = ( spec.opacity !== undefined ) ? spec.opacity : 1.0;
        this.brightness = ( spec.brightness !== undefined ) ? spec.brightness : 1.0;
        this.contrast = ( spec.brightness !== undefined ) ? spec.brightness : 1.0;
        this.enabled = ( spec.enabled !== undefined ) ? spec.enabled : true;
    }

    Layer.prototype = {

        /**
         * Set the opacity of the layer.
         * @memberof Layer.prototype
         *
         * @param {float} opacity - opacity value from 0 to 1.
         */
        setOpacity: function( opacity ) {
            this.opacity = opacity;
            if ( this.olLayer ) {
                this.olLayer.setOpacity( opacity );
            }
            PubSub.publish( this.getChannel(), { field: 'opacity', value: opacity } );
        },

        /**
         * Returns the opacity of the layer.
         * @memberof Layer.prototype
         *
         * @returns {float} The opacity of the layer.
         */
        getOpacity: function() {
            return this.opacity;
        },

        /**
         * Set the brightness of the layer.
         * @memberof Layer.prototype
         *
         * @param {float} brightness - normalized brightness value.
         */
        setBrightness: function( brightness ) {
            this.brightness = brightness;
            if ( this.olLayer ) {
                $( this.olLayer.div ).css( '-webkit-filter', "brightness("+ (this.brightness*100) +"%) contrast("+ (this.contrast*100) +"%)" );
            }
            PubSub.publish( this.getChannel(), { field: 'brightness', value: brightness } );
        },

        /**
         * Returns the brightness of the layer.
         * @memberof Layer.prototype
         *
         * @returns {float} The brightness of the layer.
         */
        getBrightness: function() {
            return this.brightness;
        },

        /**
         * Set the contrast of the layer.
         * @memberof Layer.prototype
         *
         * @param {float} contrast - normalized contrast value.
         */
        setContrast: function( contrast ) {
            this.contrast = contrast;
            if ( this.olLayer ) {
                $( this.olLayer.div ).css( '-webkit-filter', "brightness("+ (this.brightness*100) +"%) contrast("+ (this.contrast*100) +"%)" );
            }
            PubSub.publish( this.getChannel(), { field: 'contrast', value: contrast } );
        },

        /**
         * Returns the contrast of the layer.
         * @memberof Layer.prototype
         *
         * @returns {float} The contrast of the layer.
         */
        getContrast: function() {
            return this.contrast;
        },

        /**
         * Set whether or not the layer is enabled.
         * @memberof Layer.prototype
         *
         * @param enabled {boolean} whether the layer is visible or not
         */
        setEnabled: function( enabled ) {
            this.enabled = enabled;
            if ( this.olLayer ) {
                this.olLayer.setVisibility( enabled );
            }
            PubSub.publish( this.getChannel(), { field: 'enabled', value: enabled } );
        },

        /**
         * Get whether or not the layer is enabled.
         * @memberof Layer.prototype
         *
         * @returns {boolean} If the layer is visible or not.
         */
        isEnabled: function() {
            return this.enabled;
        },

        /**
         * Returns the UUID that uniquely identifies this layer.
         * @memberof Layer.prototype
         *
         * @returns {String} The UUID of the layer.
         */
        getUUID: function() {
            return this.uuid;
        },

        /**
         * Returns the publish/subscribe channel id of this specific layer.
         * @memberof Layer.prototype
         *
         * @returns {String} The publish/subscribe channel for the layer.
         */
        getChannel: function () {
            return 'layer.' + this.domain + '.' + this.uuid;
        }
    };

    module.exports = Layer;
}());
