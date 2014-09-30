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



    var //Util = require('../../../util/Util'),
        GenericHtmlRenderer = require('./GenericHtmlRenderer'),
        MAX_WORDS_DISPLAYED = 8,
        TextByFrequencyRenderer;



    TextByFrequencyRenderer = GenericHtmlRenderer.extend({
        ClassName: "TextByFrequencyRenderer",

        init: function( map, spec ) {
            this._super( map, spec );
        },


        parseInputSpec: function( spec ) {

            spec.text.textKey = spec.text.textKey || "text";
            spec.text = this.parseCountSpec( spec.text );
            spec.text = this.parseStyleSpec( spec.text );

            spec.chart = this.parseCountSpec( spec.chart );
            spec.chart = this.parseStyleSpec( spec.chart );

            return spec;
        },


        getSelectableElement: function() {
            return 'text-by-frequency-entry';
        },


        createStyles: function() {

            var spec = this.spec,
                css;

            css = '<style id="generic-text-by-frequency-renderer-css-'+this.id+'" type="text/css">';

            // generate text css
            if ( spec.text.color ) {
                css += this.generateBlendedCss( spec.text, "text-by-frequency-label", "color" );
            } else if ( spec.text.themes ) {
                css += this.generateThemedCss( spec.text, "text-by-frequency-label", "color" );
            }

            // generate chart css
            if ( spec.chart.color ) {
                css += this.generateBlendedCss( spec.chart, "text-by-frequency-bar", "background-color" );
            } else if ( spec.chart.themes ) {
                css += this.generateThemedCss( spec.chart, "text-by-frequency-bar", "background-color" );
            }

            css += '</style>';

            $( document.body ).prepend( css );
        },


        createHtml : function( data ) {

            var that = this,
                spec = this.spec,
                textKey = spec.text.textKey,
                text = spec.text,
                chart = spec.chart,
                tilekey = data.tilekey,
                html = '',
                $html,
                $elem,
                values = data.values,
                numEntries = Math.min( values.length, MAX_WORDS_DISPLAYED ),
                value, entryText,
                maxPercentage, relativePercent,
                visibility, chartSize, barClass, labelClass,
                i, j;

            /*
                Utility function for positioning the labels
            */
            function getYOffset( index, numEntries ) {
                var SPACING = 20;
                return 113 - ( (( numEntries - 1) / 2 ) - index ) * SPACING;
            }


            function getChartSize( value, subSpec ) {
                var i, chartSize = Number.MAX_VALUE;
                for ( i=0; i<subSpec.countKey.length; i++ ) {
                    chartSize = Math.min( chartSize, value[ subSpec.countKey[i] ].length );
                }
                return chartSize;
            }

            /*
                Returns the total count for single value
            */
            function getCount( value, index, subSpec ) {
                var i, count = 0;
                for ( i=0; i<subSpec.countKey.length; i++ ) {
                    count += that.getAttributeValue( value, subSpec.countKey[i] )[index];
                }
                return count;
            }

            /*
                Returns the total sum count
            */
            function getCountArraySum( value, subSpec ) {
                var countKey,
                    sum = 0, i, j;
                for ( i=0; i<subSpec.countKey.length; i++) {
                    countKey = subSpec.countKey[i];
                    for ( j=0; j<value[ countKey ].length; j++ ) {
                        sum += value[ countKey ][j];
                    }
                }
                return sum;
            }


            /*
                Returns the percentage count
            */
            function getPercentage( value, index, subSpec ) {
                return ( getCount( value, index, subSpec ) / getCountArraySum( value, subSpec ) ) || 0;
            }


            /*
                Returns the maximum percentage count
            */
            function getMaxPercentage( value, subSpec ) {
                var i,
                    percent,
                    chartSize = getChartSize( value, subSpec ),
                    maxPercent = 0,
                    count = getCountArraySum( value, subSpec );

                if (count === 0) {
                    return 0;
                }

                for (i=0; i<chartSize; i++) {
                    // get maximum percent
                    percent = getCount( value, i, subSpec ) / count;
                    if (percent > maxPercent) {
                        maxPercent = percent;
                    }
                }
                return maxPercent;
            }

            if (spec.title) {
                html += '<div class="aperture-tile-title">'+spec.title+'</div>';
            }

            $html = $(html);

            for (i=0; i<numEntries; i++) {

                value = values[i];
                entryText = this.getAttributeValue( value, textKey );
                chartSize = getChartSize( value, chart );
                maxPercentage = getMaxPercentage( value, chart );
                labelClass = this.generateBlendedClass( "text-by-frequency-label", value, text ) +"-"+this.id;

                html = '<div class="text-by-frequency-entry" style="'
                     + 'top:' +  getYOffset( i, numEntries ) + 'px;">';

                // create chart
                html += '<div class="text-by-frequency-left">';
                for (j=0; j<chartSize; j++) {
                    barClass = this.generateBlendedClass( "text-by-frequency-bar", value, chart, j ) + "-" + this.id;
                    relativePercent = ( getPercentage( value, j, chart ) / maxPercentage ) * 100;
                    visibility = (relativePercent > 0) ? '' : 'hidden';
                    relativePercent = Math.max( relativePercent, 20 );
                    // create bar
                    html += '<div class="text-by-frequency-bar '+barClass+'" style="'
                          + 'visibility:'+visibility+';'
                          + 'height:'+relativePercent+'%;'
                          + 'width:'+ Math.floor( (105+chartSize)/chartSize ) +'px;'
                          + 'top:'+(100-relativePercent)+'%;"></div>';
                }
                html += '</div>';

                // create tag label
                html += '<div class="text-by-frequency-right">';
                html += '<div class="text-by-frequency-label '+labelClass+'">'+entryText+'</div>';
                html += '</div>';
                html += '</div>';

                $elem = $( html );

                this.setMouseEventCallbacks( $elem, data, value );
                this.addClickStateClassesLocal( $elem, value, tilekey );

                $html = $html.add( $elem );
            }

            return $html;
        }


    });

    return TextByFrequencyRenderer;
});