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



    var Util = require('../../../util/Util'),
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
            spec.text.countKey = spec.text.countKey || "count";
            spec.text.blend = spec.text.blend || [{}];
            for ( i=0; i<spec.text.blend.length; i++ ) {
                spec.text.blend[i].color = spec.text.blend[i].color || DEFAULT_COLOR;
                spec.text.blend[i].hoverColor = spec.text.blend[i].hoverColor || DEFAULT_HOVER_COLOR;
                spec.text.blend[i].countKey = spec.text.blend[i].countKey || "count";
            }

            spec.chart = spec.chart || [{}];
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
                blend,
                blends,
                i,
                css;

            css = '<style id="generic-text-by-frequency-renderer-css" type="text/css">';

            // generate text css
            blends = this.generateBlendedCss( spec.text.blend );
            for ( i=0; i<blends.length; i++ ) {
                blend = blends[i];
                css += '.text-by-frequency-label'+blend.suffix+' {color:'+blend.color+';}' +
                       '.text-by-frequency-entry:hover .text-by-frequency-label'+blend.suffix+' {color:'+blend.hoverColor+';}' +
                       '.greyed .text-by-frequency-label'+blend.suffix+' {color:'+Util.hexBrightness( blend.color, 0.5 )+';}' +
                       '.clicked-secondary .text-by-frequency-label'+blend.suffix+' {color:'+blend.color+';}' +
                       '.clicked-primary .text-by-frequency-label'+blend.suffix+' {color:'+blend.hoverColor+';}';
            }

            // generate bar css
            blends = this.generateBlendedCss( spec.chart.blend );
            for ( i=0; i<blends.length; i++ ) {
                blend = blends[i];
                css += '.text-by-frequency-bar'+blend.suffix+' {background-color:'+blend.color+';}' +
                       '.text-by-frequency-entry:hover .text-by-frequency-bar'+blend.suffix+' {background-color:'+blend.hoverColor+';}' +
                       '.greyed .text-by-frequency-bar'+blend.suffix+' {background-color:'+Util.hexBrightness( blend.color, 0.5 )+';}' +
                       '.clicked-secondary .text-by-frequency-bar'+blend.suffix+' {background-color:'+blend.color+';}' +
                       '.clicked-primary .text-by-frequency-bar'+blend.suffix+' {background-color:'+blend.hoverColor+';}';
            }

            css += '</style>';

            $(document.body).prepend( css );
        },


        createHtml : function( data ) {

            var spec = this.spec,
                tilekey = data.tilekey,
                html = '',
                $html = $([]),
                $elem,
                values = data.values,
                numEntries = Math.min( values.length, MAX_WORDS_DISPLAYED ),
                value, entryText,
                maxPercentage, relativePercent,
                visibility, countArray, barClass, labelClass,
                i, j;

            function getYOffset( index, numEntries ) {
                var SPACING = 20;
                return 113 - ( (( numEntries - 1) / 2 ) - index ) * SPACING;
            }

            /*
                Returns the total sum count
            */
            function getCountArraySum( value ) {
                var countArray = value[spec.chart.countKey],
                    sum = 0, i;
                for (i=0; i<countArray.length; i++) {
                    sum += countArray[i];
                }
                return sum;
            }


            /*
                Returns the percentage count
            */
            function getPercentage( value, j ) {
                return ( value[spec.chart.countKey][j] / getCountArraySum( value ) ) || 0;
            }


            /*
                Returns the maximum percentage count
            */
            function getMaxPercentage( value, type ) {
                var i,
                    percent,
                    countArray = value[spec.chart.countKey],
                    maxPercent = 0,
                    count = getCountArraySum( value );

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

            for (i=0; i<numEntries; i++) {

                value = values[i];
                entryText = value[spec.text.textKey];
                countArray = value[spec.chart.countKey];
                maxPercentage = getMaxPercentage( value );
                labelClass = this.generateBlendedClass( "text-by-frequency-label", value, spec.text );

                html = '<div class="text-by-frequency-entry" style="top:' +  getYOffset( i, numEntries ) + 'px;">';

                // create count chart
                html +=     '<div class="text-by-frequency-left">';
                for (j=0; j<countArray.length; j++) {
                    barClass = this.generateBlendedClass( "text-by-frequency-bar", value, spec.chart, j );
                    relativePercent = ( getPercentage( value, j ) / maxPercentage ) * 100;
                    visibility = (relativePercent > 0) ? '' : 'hidden';
                    relativePercent = Math.max( relativePercent, 20 );
                    html += '<div class="text-by-frequency-bar '+barClass+'" style="visibility:'+visibility+';height:'+relativePercent+'%; width:'+ Math.floor( (105+countArray.length)/countArray.length ) +'px; top:'+(100-relativePercent)+'%;"></div>';
                }
                html +=     '</div>';

                // create tag label
                html +=     '<div class="text-by-frequency-right">';
                html +=         '<div class="text-by-frequency-label '+labelClass+'">'+entryText+'</div>';
                html +=     '</div>';

                html += '</div>';

                $elem = $(html);

                this.setMouseEventCallbacks( $elem, data, value );
                this.addClickStateClassesLocal( $elem, value, tilekey );

                $html = $html.add( $elem );
            }

            return $html;
        }


    });

    return GenericTextByFrequencyRenderer;
});