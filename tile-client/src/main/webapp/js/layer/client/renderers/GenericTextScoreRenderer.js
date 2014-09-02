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
        MAX_WORDS_DISPLAYED = 5,
        DEFAULT_COLOR = '#FFFFFF',
        DEFAULT_HOVER_COLOR = '#7FFF00',
        GenericTextScoreRenderer;



    GenericTextScoreRenderer = GenericHtmlRenderer.extend({
        ClassName: "GenericTextScoreRenderer",

        init: function( map, spec ) {
            this._super( map, spec );
        },


        parseInputSpec: function( spec ) {

            var i;

            spec.text = spec.text || {};
            spec.text.textKey = spec.text.textKey || "text";
            spec.text.countKey = spec.text.countKey || "count";
            spec.text.blend = spec.text.blend || [{}];
            for ( i=0; i<spec.text.blend.length; i++ ) {
                spec.text.blend[i].color = spec.text.blend[i].color || DEFAULT_COLOR;
                spec.text.blend[i].hoverColor = spec.text.blend[i].hoverColor || DEFAULT_HOVER_COLOR;
                spec.text.blend[i].countKey = spec.text.blend[i].countKey || "count";
            }

            spec.chart = spec.chart || {};
            spec.chart.bars = spec.chart.bars || [];
            for ( i=0; i<spec.chart.bars.length; i++ ) {
                spec.chart.bars[i].countKey = spec.chart.bars[i].countKey || "count";
                spec.chart.bars[i].color = spec.chart.bars[i].color || DEFAULT_COLOR;
                spec.chart.bars[i].hoverColor = spec.chart.bars[i].hoverColor || DEFAULT_HOVER_COLOR;
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
            return 'text-score-entry';
        },


        createStyles: function() {

            var spec = this.spec,
                css;

            css = '<style id="generic-text-score-renderer-css-'+this.id+'" type="text/css">';

            // generate text css
            css += this.generateBlendedCss( spec.text.blend, "text-score-label", "color" );

            // generate chart css
            css += this.generateCss( spec.chart.bars, "text-score-count-sub-bar-", "background-color" );

            css += '</style>';

            $( document.body ).prepend( css );
        },


        createHtml : function( data ) {

            var spec = this.spec,
                chart = spec.chart,
                bars = spec.chart.bars,
                text = spec.text,
                values = data.values,
                tilekey = data.tilekey,
                numEntries = Math.min( values.length, MAX_WORDS_DISPLAYED ),
                totalCount,
                yOffset,
                $html = $([]),
                value,
                textEntry,
                fontSize,
                percentages,
                centreIndex,
                barOffset,
                html,
                count,
                labelClass,
                $parent,
                $entry,
                i, j;

            /*
                Utility function for positioning the labels
            */
            function getYOffset( numEntries ) {
                var SPACING =  36;
                return 112 - ( ( ( numEntries - 1) / 2 ) ) * SPACING;
            }

            /*
                Returns the total sum count for all values in node
            */
            function getTotalCount( values, numEntries ) {
                var i,
                    sum = 0;
                for (i=0; i<numEntries; i++) {
                    sum += values[i][spec.text.countKey];
                }
                return sum;
            }

            /*
                Returns a font size based on the percentage of tweets relative to the total count
            */
            function getFontSize( count, totalCount ) {
                var MAX_FONT_SIZE = 22,
                    MIN_FONT_SIZE = 12,
                    FONT_RANGE = MAX_FONT_SIZE - MIN_FONT_SIZE,
                    percentage = ( count / totalCount ) || 0,
                    scale = Math.log( totalCount ),
                    size = ( percentage * FONT_RANGE * scale ) + ( MIN_FONT_SIZE * percentage );
                return Math.min( Math.max( size, MIN_FONT_SIZE), MAX_FONT_SIZE );
            }

            /*
                Maps a bar countKey to its  percentage of total count
            */
            function countToPercentage( bar ) {
                return ( value[ bar.countKey ] / count ) || 0;
            }

            totalCount = getTotalCount( values, numEntries );
            yOffset = getYOffset( numEntries );

            if ( !chart ) {
                yOffset += 10;
            }

            for (i=0; i<numEntries; i++) {

                value = values[i];
                textEntry = value[text.textKey];
                count = value[text.countKey];
                fontSize = getFontSize( count, totalCount );
                labelClass = this.generateBlendedClass( "text-score-label", value, text ) +"-"+this.id;

                if ( chart ) {
                    // get percentages for each bar
                    percentages = bars.map( countToPercentage );
                    // determine bar horizontal offset
                    centreIndex = Math.floor( bars.length / 2 );
                    barOffset = ( bars.length % 2 === 0 ) ? percentages[centreIndex] : percentages[centreIndex] / 2;
                    for (j=centreIndex-1; j>=0; j--) {
                        barOffset += percentages[j];
                    }
                }

                // create entry
                html = '<div class="text-score-entry">';
                // create label
                html += '<div class="text-score-label '+labelClass+'" style="'
                      + 'font-size:'+ fontSize +'px;'
                      + 'line-height:'+ fontSize +'px;">'+textEntry+'</div>';

                if ( chart ) {
                    // create chart
                    html += '<div class="text-score-count-bar" style="'
                          + 'left:'+ (-120*barOffset) + 'px;'
                          + 'top:'+fontSize+'px;">';
                    for (j=0; j<bars.length; j++) {
                        // create bar
                        html += '<div class="text-score-count-sub-bar '
                              + 'text-score-count-sub-bar-'+ j +'-' + this.id+'" style="'
                              + 'width:'+(percentages[j]*100)+'%;"></div>';
                    }
                    html += '</div>';
                }
                html += '</div>';

                $entry = $( html );

                $parent = $('<div class="text-score-entry-parent" style="'
                          + 'top:' + yOffset + 'px;"></div>');
                $parent.append( $entry );
                $parent = $parent.add('<div class="clear"></div>');

                this.setMouseEventCallbacks( $entry, data, value );
                this.addClickStateClassesLocal( $entry, value, tilekey );

                $html = $html.add( $parent );
            }

            return $html;
        }


    });

    return GenericTextScoreRenderer;
});