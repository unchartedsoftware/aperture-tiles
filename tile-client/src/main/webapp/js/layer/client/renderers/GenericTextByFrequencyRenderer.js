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
        DEFAULT_COLOR = '#FFFFFF',
        DEFAULT_HOVER_COLOR = '#7FFF00',
        GenericTextByFrequencyRenderer;



    GenericTextByFrequencyRenderer = GenericHtmlRenderer.extend({
        ClassName: "GenericTextByFrequencyRenderer",

        init: function( map, spec ) {
            this._super( map, spec );
        },


        parseInputSpec: function( spec ) {

            var i;
            spec.text = spec.text || {};
            spec.text.textKey = spec.text.textKey || "text";
            if ( !spec.text.blend ) {
                spec.text.blend = [{
                    countKey : spec.text.countKey,
                    color : spec.text.color,
                    hoverColor : spec.text.hoverColor
                }];
            }
            for ( i=0; i<spec.text.blend.length; i++ ) {
                spec.text.blend[i].color = spec.text.blend[i].color || DEFAULT_COLOR;
                spec.text.blend[i].hoverColor = spec.text.blend[i].hoverColor || DEFAULT_HOVER_COLOR;
                spec.text.blend[i].countKey = spec.text.blend[i].countKey || "count";
            }

            if ( !spec.chart.blend ) {
                spec.chart.blend = [{
                    countKey : spec.chart.countKey,
                    color : spec.chart.color,
                    hoverColor : spec.chart.hoverColor
                }];
            }
            for ( i=0; i<spec.chart.blend.length; i++ ) {
                spec.chart.blend[i].color = spec.chart.blend[i].color || DEFAULT_COLOR;
                spec.chart.blend[i].hoverColor = spec.chart.blend[i].hoverColor || DEFAULT_HOVER_COLOR;
                spec.chart.blend[i].countKey = spec.chart.blend[i].countKey || "count";
            }

            if ( spec.summary ) {
                if ( !$.isArray( spec.summary ) ) {
                    spec.summary = [ spec.summary ];
                }
                for ( i=0; i<spec.summary.length; i++ ) {
                    spec.summary[i].countKey = spec.summary[i].countKey || "count";
                    spec.summary[i].color = spec.summary[i].color || DEFAULT_COLOR;
                    spec.summary[i].prefix = spec.summary[i].prefix || "";
                }
            }

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
            css += this.generateBlendedCss( spec.text.blend, "text-by-frequency-label", "color" );

            // generate bar css
            css += this.generateBlendedCss( spec.chart.blend, "text-by-frequency-bar", "background-color" );

            css += '</style>';

            $( document.body ).prepend( css );
        },


        createHtml : function( data ) {

            var spec = this.spec,
                textKey = spec.text.textKey,
                text = spec.text.blend || [ spec.text ],
                chart = spec.chart.blend || [ spec.chart ],
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
                for ( i=0; i<subSpec.length; i++ ) {
                    chartSize = Math.min( chartSize, value[ subSpec[i].countKey ].length );
                }
                return chartSize;
            }

            /*
                Returns the total count for single value
            */
            function getCount( value, index, subSpec ) {
                var i, count = 0;
                for ( i=0; i<subSpec.length; i++ ) {
                    count += value[ subSpec[i].countKey ][index];
                }
                return count;
            }

            /*
                Returns the total sum count
            */
            function getCountArraySum( value, subSpec ) {
                var countKey,
                    sum = 0, i, j;
                for ( i=0; i<subSpec.length; i++) {
                    countKey = subSpec[i].countKey;
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
                html += '<div class="text-by-frequency-title">'+spec.title+'</div>';
            }
            $html = $(html);

            for (i=0; i<numEntries; i++) {

                value = values[i];
                entryText = value[ textKey ];
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

    return GenericTextByFrequencyRenderer;
});