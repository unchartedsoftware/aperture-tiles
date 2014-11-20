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


define( function () {
    "use strict";

    var evaluateCss,
        evaluateHtml;

    evaluateCss = function( data, css ) {
        var result = {},
            key;
        for (key in css) {
            if (css.hasOwnProperty(key)) {
                // set as value or evaluate function
                result[key] = ( $.isFunction( css[key] ) )
                    ? $.proxy( css[key], data )()
                    : css[key];
            }
        }
        return result;
    };

    evaluateHtml = function( node, html, css ) {
        var $html;
        // create and style html elements
        $html = $.isFunction( html ) ? $.proxy( html, node.data )() : html;
        $html = ( $html instanceof jQuery ) ? $html : $($html);
        $html.css( evaluateCss( node.data, css ) );
        return $html;
    };

    function HtmlLayer( spec ) {
        this.html_ = spec.html || null;
        this.css_ = spec.css || {};
    }

    HtmlLayer.prototype.html = function( html ) {
        // set or update the internal html of this layer
        this.html_ = html;
    };

    HtmlLayer.prototype.css = function( attribute, value ) {
        // add css object or attribute to css for layer
        if ( $.isPlainObject(attribute) ) {
            $.extend( this.css_, attribute );
        } else {
            this.css_[attribute] = value;
        }
    };

    HtmlLayer.prototype.redraw = function( nodes ) {
        var i,
            node;
        for (i=0; i<nodes.length; i++) {
            node = nodes[i];
            // create elements
            node.$elements = evaluateHtml( node, this.html_, this.css_ );
            // append elements to tile root
            node.$root.append( node.$elements );
        }
    };

    return HtmlLayer;
});
