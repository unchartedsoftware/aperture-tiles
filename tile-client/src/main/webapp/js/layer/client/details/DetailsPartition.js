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
        DetailsPartition;



    DetailsPartition = GenericDetailsElement.extend({
        ClassName: "DetailsPartition",

        init: function( spec ) {
            this._super( spec );

            this.$element = null;
        },

        parseInputSpec: function( spec ) {
            spec.width = spec.width || "100%";
            spec.height = spec.height || "50%";
            return spec;
        },

        createStyles: function() {
            return true;
        },

        create: function( value ) {

            var html = '', i;

            html += '<div class="details-partition" style="'
                  + 'width:'+this.spec.width+';'
                  + 'height:'+this.spec.height+'";></div>';

            this.$element = $( html );

            for ( i=0; i<this.content.length; i++ ) {
                this.$element.append( this.content[i].create( value ) );
            }

            return this.$element ;
        },


        destroy : function() {
            if ( this.$element ) {
                this.$element.remove();
                this.$element = null;
            }
        }

    });

    return DetailsPartition;

});