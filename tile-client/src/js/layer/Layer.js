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
 * A layer interface that stores common functionality across all layer types.
 */
( function() {

    "use strict";

    var Util = require('../util/Util'),
        PubSub = require('../util/PubSub');

    function Layer( spec ) {
        this.uuid = Util.generateUuid();
        this.name = spec.name || "Unnamed Layer";
        this.domain = spec.domain;
        this.map = spec.map;
        spec.opacity = ( spec.opacity !== undefined ) ? spec.opacity : 1.0;
        spec.enabled = ( spec.enabled !== undefined ) ? spec.enabled : true;
        this.spec = spec;
    }

    Layer.prototype = {

        /**
         * Set the opacity of the layer
         *
         * @param opacity {float} opacity value from 0 to 1
         */
        setOpacity: function( opacity ) {
            this.spec.opacity = opacity;
            if ( this.olLayer ) {
                this.olLayer.setOpacity ( opacity );
                PubSub.publish( this.getChannel(), { field: 'opacity', value: opacity } );
            }
        },

        /**
         * Returns the opacity of the layer.
         */
        getOpacity: function() {
            return this.spec.opacity;
        },

        /**
         * Set the visibility of the layer.
         *
         * @param visibility {boolean} whether the layer is visible or not
         */
        setVisibility: function( visibility ) {
            this.spec.enabled = visibility;
            if ( this.olLayer ) {
                this.olLayer.setVisibility(visibility);
                PubSub.publish( this.getChannel(), { field: 'enabled', value: visibility } );
            }
        },

        /**
         * Returns the visibility of the layer.
         */
        getVisibility: function() {
            return this.spec.enabled;
        },

        /**
         * Returns the UUID that uniquely identifies this layer.
         */
        getUUID: function() {
            return this.uuid;
        },

        /**
         * Returns the publish/subscribe channel id of this specific layer.
         */
        getChannel: function () {
            return 'layer.' + this.domain + '.' + this.uuid;
        }
    };

    module.exports = Layer;
}());
