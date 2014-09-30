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
        TextScoreRenderer;



    TextScoreRenderer = GenericHtmlRenderer.extend({
        ClassName: "TextScoreRenderer",

        init: function( map, spec ) {
            this._super( map, spec );
        },


        parseInputSpec: function( spec ) {

            var i;

            spec.text.textKey = spec.text.textKey || "text";
            spec.text = this.parseCountSpec( spec.text );
            spec.text = this.parseStyleSpec( spec.text );

            if ( spec.chart ) {
                for ( i=0; i<spec.chart.bars.length; i++ ) {
                    spec.chart.bars[i] = this.parseCountSpec( spec.chart.bars[i] );
                    spec.chart.bars[i] = this.parseStyleSpec( spec.chart.bars[i] );
                }
            }

            return spec;
        },


        getSelectableElement: function() {
            return 'text-score-entry';
        },


        addBarBorderCss: function( barSpec ) {
            var themeName,
                css = "";
            if ( barSpec.color ) {
                css += ".text-score-count-bar { border: 1px solid"+ barSpec.outline + ";}";
                // remove outline from the spec here so that it isn't added to the sub-bars css later
                delete barSpec.outline;
            } else if ( barSpec.themes ) {
                for ( themeName in barSpec.themes ) {
                    if ( barSpec.themes.hasOwnProperty( themeName ) ) {
                        css += "."+themeName+" .text-score-count-bar { border: 1px solid"+ barSpec.themes[ themeName ].outline + ";}";
                        // remove outline from the spec here so that it isn't added to the sub-bars css later
                        delete barSpec.themes[ themeName ].outline;
                    }
                }
            }
            return css;
        },

        createStyles: function() {

            var spec = this.spec,
                css, i;

            css = '<style id="generic-text-score-renderer-css-'+this.id+'" type="text/css">';

            // generate text css
            if ( spec.text.color ) {
                css += this.generateBlendedCss( spec.text, "text-score-label", "color" );
            } else if ( spec.text.themes ) {
                css += this.generateThemedCss( spec.text, "text-score-label", "color" );
            }

            // generate chart css
            if ( spec.chart ) {
                for ( i=0; i<spec.chart.bars.length; i++ ) {

                    // call this first, to remove outline from spec
                    css += this.addBarBorderCss( spec.chart.bars[i] );

                    if ( spec.chart.bars[i].color ) {
                        css += this.generateBlendedCss( spec.chart.bars[i], "text-score-count-sub-bar-"+i, "background-color" );
                    } else if ( spec.chart.bars[i].themes ) {
                        css += this.generateThemedCss( spec.chart.bars[i], "text-score-count-sub-bar-"+i, "background-color" );
                    }
                }
            }

            css += '</style>';

            $( document.body ).prepend( css );
        },


        createHtml : function( data ) {

            var that = this,
                spec = this.spec,
                chart = spec.chart,
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
                textCount,
                chartCount,
                labelClass,
                barClass,
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
                Returns the total count for single value
            */
            function getCount( value, subSpec ) {
                var i, count = 0;
                for ( i=0; i<subSpec.countKey.length; i++ ) {
                    count += that.getAttributeValue( value, subSpec.countKey[i] );
                }
                return count;
            }


            function getBarCount( value, subSpec ) {
                var i, j, count = 0;
                for ( i=0; i<subSpec.bars.length; i++ ) {
                    for ( j=0; j<subSpec.bars[i].countKey.length; j++ ) {
                        count += that.getAttributeValue( value, subSpec.bars[i].countKey[j] );
                    }
                }
                return count;
            }

            /*
                Returns the total sum count for all values in node
            */
            function getTotalCount( values, numEntries, subSpec ) {
                var i,
                    sum = 0;
                for (i=0; i<numEntries; i++) {
                    sum += getCount( values[i], subSpec );
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
                return ( value[ bar.countKey ] / chartCount ) || 0;
            }

            if (spec.title) {
                $html = $html.add('<div class="aperture-tile-title">'+spec.title+'</div>');
            }

            totalCount = getTotalCount( values, numEntries, text );
            yOffset = getYOffset( numEntries );

            if ( !chart ) {
                yOffset += 10;
            }

            for (i=0; i<numEntries; i++) {

                value = values[i];
                textEntry = this.getAttributeValue( value, text.textKey );
                textCount = getCount( value, text );
                fontSize = getFontSize( textCount, totalCount );
                labelClass = this.generateBlendedClass( "text-score-label", value, text ) +"-"+this.id;

                if ( chart ) {

                    chartCount = getBarCount( value, chart );
                    // get percentages for each bar
                    percentages = chart.bars.map( countToPercentage );
                    // determine bar horizontal offset
                    centreIndex = Math.floor( (chart.bars.length-1) / 2 );
                    barOffset = ( chart.bars.length % 2 === 0 ) ? percentages[centreIndex] : percentages[centreIndex] / 2;
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
                    for (j=0; j<chart.bars.length; j++) {
                        // create bar
                        barClass = this.generateBlendedClass( "text-score-count-sub-bar-"+j, value, chart.bars[j] ) +"-"+this.id;
                        html += '<div class="text-score-count-sub-bar '
                              //+ 'text-score-count-sub-bar-'+ j +'-' + this.id+'" style="'
                              + barClass + '" style="'
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

    return TextScoreRenderer;
});