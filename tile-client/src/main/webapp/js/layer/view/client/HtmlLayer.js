/*
 * Copyright (c) 2013 Oculus Info Inc.
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

/* JSLint global declarations: these objects don't need to be declared. */
/*global OpenLayers */

/**
 * This module defines the base class for a client render layer. Must be 
 * inherited from for any functionality.
 */
define(function (require) {
    "use strict";



    var Class = require('../../../class'),
        HtmlLayer;



    HtmlLayer = Class.extend({
        ClassName: "HtmlLayer",

        /**
         * Constructs a client render layer object
         * @param id the id string for the render layer
         */
        init: function( spec ) {

            this.html_ = spec.html || null;
            this.css_ = spec.css || {};
            this.nodeLayer_ = null;
        },


        html : function( html ) {
            // set or update the internal html of this layer
            this.html_ = html;
            this.update();
        },


        css : function( attribute, value ) {
            // add css object or attribute to css for layer
            if ( $.isPlainObject(attribute) ) {
                $.extend(this.css_, attribute);
            } else {
                this.css_[attribute] = value;
            }
        },


        evalCss: function(data) {

            var result = {},
                key;

            for (key in this.css_) {
                if (this.css_.hasOwnProperty(key)) {
                    // set as value or evaluate function
                    result[key] = ( $.isFunction( this.css_[key] ) )
                        ? $.proxy( this.css_[key], data )()
                        : this.css_[key];
                }
            }
            return result;
        },


        redrawNode: function( node ) {

            var html,
                $elem;

            // create and style html elements
            html = $.isFunction( this.html_ ) ? $.proxy( this.html_, node.data )() : this.html_;
            $elem = ( html instanceof jQuery ) ? html : $(html);
            $elem.css( this.evalCss( node.data ) );

            // append to tile root
            node.$root.append( $elem );

        },


        redraw :  function( nodes ) {
            var i;
            for (i=0; i<nodes.length; i++) {
                this.redrawNode( nodes[i] );
            }
        }

    });

    return HtmlLayer;
});
