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



    var GenericHtmlRenderer = require('./GenericHtmlRenderer'),
        MetaDataRenderer;



    MetaDataRenderer = GenericHtmlRenderer.extend({
        ClassName: "MetaDataRenderer",

        init: function( map, spec ) {
            this._super( map, spec );
        },

        getSelectableElement: function() {
            return 'meta-entry';
        },

        parseInputSpec: function( spec ) {

            spec.text.metaKeys = spec.text.metaKeys || [ "source" ];
            spec.text = this.parseStyleSpec( spec.text );
            return spec;
        },

        createStyles: function() {
            var spec = this.spec,
                css;

            css = '<style id="generic-meta-data-renderer-css-'+this.id+'" type="text/css">';

            if ( spec.text.color ) {
                css += this.generateBlendedCss( spec.text, "meta-entry-label", "color" );
            } else if ( spec.text.themes ) {
                css += this.generateThemedCss( spec.text, "meta-entry-label", "color" );
            }

            css += '</style>';

            $( document.body ).prepend( css );
        },

        createHtml : function( data ) {

            /*
                Utility function for positioning the labels
            */
            function getYOffset( numEntries ) {
                var SPACING =  36;
                return 108 - ( ( ( numEntries - 1) / 2 ) ) * SPACING;
            }

            /*
                Depth-first search of meta data for specified keys
            */
            function findKeyVals( meta, keys, arr ) {

                var key;
                if ( arr === undefined ) {
                    arr = [];
                }
                for ( key in meta ) {
                    if( meta.hasOwnProperty( key ) ){

                        if ( $.isPlainObject( meta[key] ) ) {
                            arr = findKeyVals( meta[key], keys, arr );
                        } else {
                            if ( keys.indexOf(key) !== -1 ) {
                                arr.push( { key: key, value: meta[key] } );
                            }
                        }
                    }
                }
                return arr;
            }


            var spec = this.spec,
                FONT_SIZE = 16,
                meta = data.meta,
                keyVals = findKeyVals( meta, spec.text.metaKeys ),
                yOffset = getYOffset( keyVals.length ),
                $html = $([]),
                html,
                $parent,
                i;

            for (i=0; i<keyVals.length; i++) {

                html =     '<div class="meta-entry">';
                html +=         '<div class="meta-entry-label-100-'+this.id+'" style="font-size:' + FONT_SIZE + 'px;">'+keyVals[i].key+':</div>';
                html +=         '<div class="meta-entry-label-100-'+this.id+'" style="font-size:' + FONT_SIZE + 'px; margin-bottom: 5px;">'+keyVals[i].value+'</div>';
                html +=    '</div>';

                $parent = $('<div class="meta-entry-parent" style="top:' + yOffset + 'px;"></div>');
                $parent.append( html );
                $parent = $parent.add('<div class="clear"></div>');

                $html = $html.add( $parent );
            }

            return $html;

        }


    });

    return MetaDataRenderer;
});