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



    var HtmlNodeLayer = require('../../HtmlNodeLayer'),
        HtmlLayer = require('../../HtmlLayer'),
        GenericHtmlRenderer = require('./GenericHtmlRenderer'),
        //Util = require('../../../util/Util'),
        GenericMetaDataRenderer;



    GenericMetaDataRenderer = GenericHtmlRenderer.extend({
        ClassName: "GenericMetaDataRenderer",

        init: function( map, spec ) {

            this._super( map, spec );
            this.createNodeLayer(); // instantiate the node layer data object
            this.createLayer();     // instantiate the html visualization layer
        },


        addClickStateClassesGlobal: function() {

            var selectedValue = this.layerState.get('click')[this.spec.valueKey],
                $elements = $(".text-score-entry");

            $elements.filter( function() {
                return $(this).text() !== selectedValue;
            }).addClass('greyed').removeClass('clicked');

            $elements.filter( function() {
                return $(this).text() === selectedValue;
            }).removeClass('greyed').addClass('clicked');

        },


        removeClickStateClassesGlobal: function() {

            $(".text-score-entry").removeClass('greyed clicked');
        },


        createNodeLayer: function() {

            /*
                 Instantiate the html node layer. This holds the tile data as it comes in from the tile service. Here
                 we set the x and y coordinate mappings that are used to position the individual nodes on the map. In this
                 example, the data is geospatial and is under the keys 'latitude' and 'longitude'. The idKey
                 attribute is used as a unique identification key for internal managing of the data. In this case, it is
                 the tilekey.
             */
            this.nodeLayer = new HtmlNodeLayer({
                map: this.map,
                xAttr: 'longitude',
                yAttr: 'latitude',
                idKey: 'tilekey'
            });
        },


        createLayer : function() {

            var spec = this.spec;

            /*
                Utility function for positioning the labels
            */
            function getYOffset( numEntries ) {
                var SPACING =  36;
                return 90 - ( ( ( numEntries - 1) / 2 ) ) * SPACING;
            }

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

            /*
                Here we create and attach an individual html layer to the html node layer. For every individual node
                of data in the node layer, the html function will be executed with the 'this' context that of the node.
             */
            this.nodeLayer.addLayer( new HtmlLayer({

                html: function() {

                    var FONT_SIZE = 20,
                        meta = this.meta,
                        tilekey = this.tilekey,
                        keyVals = findKeyVals( meta, spec.keys ),
                        yOffset = getYOffset( keyVals.length ),
                        $html = $('<div class="aperture-tile aperture-tile-'+tilekey+'"></div>'),
                        html,
                        $parent,
                        $entry,
                        i;

                    for (i=0; i<keyVals.length; i++) {

                        $parent = $('<div class="meta-entry-parent" style="top:' + yOffset + 'px;"></div>');

                        html =     '<div class="meta-entry">';
                        // create entry label
                        html +=         '<div class="meta-entry-label" style="font-size:' + FONT_SIZE + 'px;">'+keyVals[i].key+':</div>';
                        html +=         '<div class="meta-entry-label" style="font-size:' + FONT_SIZE + 'px;">'+keyVals[i].value+'</div>';

                        html +=    '</div>';

                        $entry = $(html);

                        $parent.append( $entry );
                        $parent = $parent.add('<div class="clear"></div>');

                        //that.setMouseEventCallbacks( $entry, this, value, spec.entryKey, spec.countKey );
                        //that.addClickStateClasses( $entry, value, spec.entryKey );

                        $html.append( $parent );
                    }

                    // return the jQuery object. You can also return raw html as a string.
                    return $html;
                }
            }));

        }


    });

    return GenericMetaDataRenderer;
});