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
        MAX_WORDS_DISPLAYED = 5,
        GenericTextScoreRenderer;



    GenericTextScoreRenderer = GenericHtmlRenderer.extend({
        ClassName: "GenericTextScoreRenderer",

        init: function( map, spec ) {

            this._super( map, spec );
            this.createNodeLayer(); // instantiate the node layer data object
            this.createLayer();     // instantiate the html visualization layer
        },


        addClickStateClassesGlobal: function() {

            var selectedValue = this.layerState.get('click')[this.spec.entryKey],
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

            var that = this,
                spec = this.spec;

            /*
                Utility function for positioning the labels
            */
            function getYOffset( numEntries ) {
                var SPACING =  36;
                return 112 - ( ( ( numEntries - 1) / 2 ) ) * SPACING;
            }


            /*
                Returns the total count of all tweets in a node
            */
            function getTotalCount( values, numEntries ) {

                var i,
                    sum = 0;
                for (i=0; i<numEntries; i++) {
                    sum += values[i][spec.countKey];
                }
                return sum;
            }


            /*
                Returns the percentage of tweets in a node for the respective tag
            */
            function getTotalCountPercentage( values, value, numEntries ) {
                return ( value[spec.countKey] / getTotalCount( values, numEntries ) ) || 0;
            }


            /*
                Returns a font size based on the percentage of tweets relative to the total count
            */
            function getFontSize( values, value, numEntries, minFontSize, maxFontSize ) {
                var fontRange = maxFontSize - minFontSize,
                    sum = getTotalCount( values, numEntries ),
                    percentage = getTotalCountPercentage( values, value, numEntries ),
                    scale = Math.log( sum ) * 0.5,
                    size = ( percentage * fontRange * scale ) + ( minFontSize * percentage );
                return Math.min( Math.max( size, minFontSize), maxFontSize );
            }


            /*
                Here we create and attach an individual html layer to the html node layer. For every individual node
                of data in the node layer, the html function will be executed with the 'this' context that of the node.
             */
            this.nodeLayer.addLayer( new HtmlLayer({

                html: function() {

                    var MAX_FONT_SIZE = 22,
                        MIN_FONT_SIZE = 12,
                        values = this.bin.value,
                        tilekey = this.tilekey,
                        numEntries = Math.min( values.length, MAX_WORDS_DISPLAYED ),
                        yOffset = getYOffset( numEntries ),
                        $html = $('<div class="aperture-tile aperture-tile-'+tilekey+'"></div>'),
                        value,
                        entryText,
                        fontSize,
                        subCounts,
                        html,
                        count,
                        $parent,
                        $entry,
                        i, j;

                    if ( !spec.subCountKeys ) {
                        yOffset += 10;
                    }

                    $html.append('<div class="count-summary"></div>');

                    for (i=0; i<numEntries; i++) {

                        value = values[i];

                        entryText = value[spec.entryKey];
                        count = value[spec.countKey];

                        subCounts = [];
                        if ( spec.subCountKeys ) {
                            for (j=0; j<spec.subCountKeys.length; j++) {
                                subCounts.push( value[spec.subCountKeys[j]] );
                            }
                        }

                        fontSize = getFontSize( values, value, numEntries, MIN_FONT_SIZE, MAX_FONT_SIZE );

                        $parent = $('<div class="text-score-entry-parent" style="top:' + yOffset + 'px;"></div>');

                        html =     '<div class="text-score-entry">';
                        // create entry label
                        html +=         '<div class="text-score-entry-label" style="font-size:' + fontSize +'px;">'+entryText+'</div>';

                        // create entry count bars
                        if ( spec.subCountKeys ) {
                            html +=         '<div class="text-score-count-bar" style="left:-60px; top:'+fontSize+'px;">';
                            for (j=0; j<subCounts.length; j++) {
                                html +=         '<div class="text-score-count-sub-bar" style="width:'+((subCounts[j]/count)*100)+'%;"></div>';
                            }
                            html +=         '</div>';
                        }

                        html +=     '</div>';

                        $entry = $(html);

                        $parent.append( $entry );
                        $parent = $parent.add('<div class="clear"></div>');

                        that.setMouseEventCallbacks( $entry, this, value, spec.entryKey, spec.countKey );
                        that.addClickStateClasses( $entry, value, spec.entryKey );

                        $html.append( $parent );
                    }

                    // return the jQuery object. You can also return raw html as a string.
                    return $html;
                }
            }));

        }


    });

    return GenericTextScoreRenderer;
});