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


define(function (require) {
    "use strict";



    var GenericDetailsElement = require('./GenericDetailsElement'),
        DetailsIFrame;



    DetailsIFrame = GenericDetailsElement.extend({
        ClassName: "DetailsIFrame",

        init: function( spec ) {
            this._super( spec );

            this.$element = null;
        },

        parseInputSpec: function( spec ) {
            spec.width = spec.width || "255";
            spec.height = spec.height || "300";
            return spec;
        },

        createStyles: function() {
            return true;
        },

        create: function( value ) {

            var html = '',
                i;

            for (i=0; i<value.length; i++) {
                html += '<iframe style="'
                      + 'display:inline;"'
                      + 'src="'
                      + value[i].key + 'embed/'
                      + '" width="'+this.spec.width+'"'
                      + 'height="'+this.spec.height+'"'
                      + 'frameborder="0"'
                      + 'scrolling="no"'
                      + 'allowtransparency="true"></iframe>';
            }

            this.$element = $( html );
            return this.$element;
        },


        destroy : function() {
            if ( this.$element ) {
                this.$element.remove();
                this.$element = null;
            }
        }

    });

    return DetailsIFrame;

});