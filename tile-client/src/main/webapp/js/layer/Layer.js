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

        setOpacity: function( opacity ) {
            this.spec.opacity = opacity;
            if ( this.layer ) {
                this.layer.setOpacity ( opacity );
                PubSub.publish( this.getChannel(), { field: 'opacity', value: opacity } );
            }
        },

        getOpacity: function() {
            return this.spec.opacity;
        },

        setVisibility: function( visibility ) {
            this.spec.enabled = visibility;
            if ( this.layer ) {
                this.layer.setVisibility(visibility);
                PubSub.publish( this.getChannel(), { field: 'enabled', value: visibility } );
            }
        },

        getVisibility: function() {
            return this.spec.enabled;
        },

        getChannel: function () {
            return 'layer.' + this.domain + '.' + this.uuid;
        },

        getURL: function( bounds ) {
            var res = this.map.getResolution(),
                maxBounds = this.maxExtent,
                tileSize = this.tileSize,
                x = Math.round( (bounds.left-maxBounds.left) / (res*tileSize.w) ),
                y = Math.round( (bounds.bottom-maxBounds.bottom) / (res*tileSize.h) ),
                z = this.map.getZoom();
            if ( x >= 0 && y >= 0 ) {
                return this.url + this.layername + "/" + z + "/" + x + "/" + y + "." + this.type;
            }
        }
    };
    return Layer;
});
