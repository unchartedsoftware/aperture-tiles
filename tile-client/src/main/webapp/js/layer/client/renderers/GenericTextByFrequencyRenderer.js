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
        MAX_WORDS_DISPLAYED = 8,
        GenericTextByFrequencyRenderer;



    GenericTextByFrequencyRenderer = GenericHtmlRenderer.extend({
        ClassName: "GenericTextByFrequencyRenderer",

        init: function( map, spec ) {

            this._super( map, spec );
            this.createNodeLayer(); // instantiate the node layer data object
            this.createLayer();     // instantiate the html visualization layer
        },


        addClickStateClassesGlobal: function() {

            var selectedValue = this.layerState.get('click')[this.spec.entryKey],
                $elements = $(".text-by-frequency-entry");

            $elements.filter( function() {
                return $(this).text() !== selectedValue;
            }).addClass('greyed').removeClass('clicked');

            $elements.filter( function() {
                return $(this).text() === selectedValue;
            }).removeClass('greyed').addClass('clicked');
        },


        removeClickStateClassesGlobal: function() {

            $(".text-by-frequency-entry").removeClass('greyed clicked');
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

            function getYOffset( index, numEntries ) {
                var SPACING = 20;
                return 113 - ( (( numEntries - 1) / 2 ) - index ) * SPACING;
            }

            /*
                Returns the total count of all tweets in a node
            */
            function getTotalCount( value ) {

                var countArray = value[spec.countKey],
                    sum = 0, i;
                for (i=0; i<countArray.length; i++) {
                    sum += countArray[i];
                }
                return sum;
            }


            /*
                Returns the percentage of tweets in a node for the respective tag
            */
            function getPercentage( value, j ) {
                return ( value[spec.countKey][j] / getTotalCount( value ) ) || 0;
            }


            function getMaxPercentage( value, type ) {
                var i,
                    percent,
                    countArray = value[spec.countKey],
                    maxPercent = 0,
                    count = getTotalCount( value );

                if (count === 0) {
                    return 0;
                }
                for (i=0; i<countArray.length; i++) {
                    // get maximum percent
                    percent = countArray[i] / count;
                    if (percent > maxPercent) {
                        maxPercent = percent;
                    }
                }
                return maxPercent;
            }

            this.nodeLayer.addLayer( new HtmlLayer({

                html: function() {

                    var tilekey = this.tilekey,
                        html = '',
                        $html = $('<div id="'+tilekey+'" class="aperture-tile"></div>'),                     
                        $elem,
                        values = this.bin.value,
                        numEntries = Math.min( values.length, MAX_WORDS_DISPLAYED ),
                        value, entryText,
                        maxPercentage, relativePercent,
                        visibility, countArray,
                        i, j;

                    for (i=0; i<numEntries; i++) {

                        value = values[i];

                        entryText = value[spec.entryKey];
                        countArray = value[spec.countKey];

                        maxPercentage = getMaxPercentage( value );

                        html = '<div class="text-by-frequency-entry" style="top:' +  getYOffset( i, numEntries ) + 'px;">';

                        // create count chart
                        html += '<div class="text-by-frequency-left">';
                        for (j=0; j<countArray.length; j++) {
                            relativePercent = ( getPercentage( value, j ) / maxPercentage ) * 100;
                            visibility = (relativePercent > 0) ? '' : 'hidden';
                            relativePercent = Math.max( relativePercent, 20 );
                            html += '<div class="text-by-frequency-bar" style="visibility:'+visibility+';height:'+relativePercent+'%; width:'+ Math.floor( (105+countArray.length)/countArray.length ) +'px; top:'+(100-relativePercent)+'%;"></div>';
                        }
                        html += '</div>';

                        // create tag label
                        html += '<div class="text-by-frequency-right">';
                        html +=     '<div class="text-by-frequency-label">'+entryText+'</div>';
                        html += '</div>';

                        html += '</div>';

                        $elem = $(html);

                        that.setMouseEventCallbacks( $elem, this, value, spec.entryKey );
                        that.addClickStateClasses( $elem, value, spec.entryKey );

                        $html.append( $elem );
                    }

                    return $html;
                }
            }));

        }


    });

    return GenericTextByFrequencyRenderer;
});