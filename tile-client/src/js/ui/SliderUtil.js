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
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

( function() {

    'use strict';

    var AxisUtil = require('../map/AxisUtil');

    module.exports = {

        /**
         * Creates and appends a label to the supplied element.
         *
         * @param {jQuery} $elem - The element to which the label is appended onto.
         * @param {number} value - The value to be written on the label.
         */
        createLabel: function( $elem, value ) {
            var options = {
                    'stepDown' : true,
                    'decimals' : 2,
                    'type': 'b'
                },
                $label = $('<div class="hover-label" style="left:'+ ($elem.width()/2) +'px; top:0px;">'+
                              '<div class="hover-label-text">'+ AxisUtil.formatText( value, options ) +'</div>'+
                           '</div>');
            // remove previous label if it exists
            $elem.find('.hover-label').finish().remove();
            // add new label
            $elem.append( $label );
            // reposition to be centred above cursor
            $label.css( {"margin-top": -$label.outerHeight()*1.2, "margin-left": -$label.outerWidth()/2 } );
        },

        /**
         * Removes a label from the element. Fades the element out first.
         *
         * @param {jQuery} $elem - The element to which the label is appended onto.
         */
        removeLabel: function( $elem ) {
            $elem.find('.hover-label').animate({
                    opacity: 0
                },
                {
                    complete: function() {
                        $elem.find('.hover-label').remove();
                    },
                    duration: 800
                }
            );
        }

    };

}());
