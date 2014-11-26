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
        MAX_FONT_SIZE = 22,
        MIN_FONT_SIZE = 12,
        FONT_RANGE = MAX_FONT_SIZE - MIN_FONT_SIZE,
        GraphNodeRenderer;



    GraphNodeRenderer = GenericHtmlRenderer.extend({
        ClassName: "GraphNodeRenderer",

        init: function( map, spec ) {
            this._super( map, spec );
        },


        parseInputSpec: function( spec ) {
            spec.node = this.parseStyleSpec( spec.node );
            return spec;
        },


        getSelectableElement: function() {
            return 'graph-label-entry';
        },


        createThemedCss: function( subSpec, className ) {
            var theme,
                themeName,
                css = "";

            for ( themeName in subSpec.themes ) {
                if ( subSpec.themes.hasOwnProperty( themeName ) ) {

                    theme = subSpec.themes[ themeName ];

                    if ( theme.gradient ) {
                        css += this.createGradientCss( theme, className, themeName );
                    } else if ( theme.color ) {
                        css += this.createUnThemedCss( theme, className );
                    }
                }
            }
            return css;
        },


        createUnThemedCss: function( subSpec, className ) {
            var css = "";
            css += '.' +className + '{ color: ' + subSpec.color + '; '+ this.generateOutlineCss( subSpec, 'color')+'}';
            css += '.' +className + ':hover { color: ' + subSpec.hoverColor + '; '+ this.generateOutlineCss( subSpec, 'color')+'}';
            return css;
        },


        createGradientCss: function( subSpec, className, theme ) {
            var css = "",
                themeClass = "",
                i;
            if ( theme ) {
                themeClass = '.' + theme;
            }

            for ( i=0; i<=10; i++ ) {
                css += themeClass + ' .' + className + '-'+(i*10)+' { color: ' + Util.hexPercentageBlend( subSpec.gradient, [ ( 10-i ) / 10, i / 10 ]  ) + ';' + this.generateOutlineCss( subSpec, 'color')+'}';
                css += themeClass + ' .' + className + '-'+(i*10)+':hover { color: ' + subSpec.hoverColor + ';' + this.generateOutlineCss( subSpec, 'color')+'}';
            }

            return css;
        },


        createStyles: function() {

            var spec = this.spec,
                css;

            css = '<style id="generic-graph-label-renderer-css-'+this.id+'" type="text/css">';

            if ( spec.node.color ) {
                css += this.createUnThemedCss( spec.node, 'node-label-' + this.id );
            } else if ( spec.node.themes ) {
                css += this.createThemedCss( spec.node, 'node-label-' + this.id );
            } else if ( spec.node.gradient ) {
                css += this.createThemedCss( spec.node, 'node-label-' + this.id );
            }

            css += '</style>';

            $( document.body ).prepend( css );
        },


        createHtml : function( data ) {

            var spec = this.spec,
                meta = this.meta[ this.map.getZoom() ],
                values = data.values,
                tilekey = data.tilekey,
                value, community,
                tilePos = this.map.getTopLeftViewportPixelForTile( tilekey ),
                pos, offset,
                $html = $([]),
                fontSize,
                split,
                countNorm,
                labelIndex,
                percent,
                weight,
                hierLevel,
                parentIDarray,
                i, j;

            function getFontSize( count, totalCount ) {
                var percentage = ( count / totalCount ) || 0,
                    size = ( percentage * FONT_RANGE  ) +  MIN_FONT_SIZE;
                return Math.min( Math.max( size, MIN_FONT_SIZE ), MAX_FONT_SIZE );
            }

            function capitalize( str ) {
            	return str.replace(/(?:^|,|\s)\S/g, function(a) { return a.toUpperCase(); });	//convert metadata string to camel-case
            }

            //get graph hierarchy level for this zoom level (assumed same hierarchy level for all tiles at a given zoom level)
            hierLevel = meta.minMax.max.communities[0].hierLevel;	
            
            if (hierLevel === 0) {
            	countNorm = meta.minMax.max.communities[0].degree / 2;	//normalize label attributes by community degree if hierLevel = 0
            }
            else {
            	countNorm = meta.minMax.max.communities[0].numNodes / 2;	//else normalize label attributes by num internal nodes
            }
            	            
            for (i=0; i<values.length; i++) {

                value = values[i];
                parentIDarray = [];	//re-init this array for each tile

                for (j=0; j<value.communities.length; j++) {

                    community = value.communities[j];
                    
                    pos = this.map.getViewportPixelFromCoord( community[spec.node.x], community[spec.node.y] );
                    
                    offset = {
                        x : pos.x - tilePos.x,
                        y : pos.y - tilePos.y
                    };
                     
                    split = capitalize( community.metadata.toLowerCase() ).split(",");
                    labelIndex = ( spec.labelIndex !== undefined ) ? spec.labelIndex : 0;

                    // Only draw one label per Parent community per tile, and don't draw labels for isolated communities,
                    // and don't render if label string is empty, and only draw up to 5 labels per tile
                    if ((community.degree === 0) 
                    		|| (split[labelIndex] === "")
                    		|| (parentIDarray.length >= 5)
                    		|| (parentIDarray.indexOf(community.parentID) !== -1)) {	
                    	continue;	// Skip this label entry
                    }
                    else {
                    	parentIDarray.push(community.parentID);	// add this parent ID to the list, and draw the label (below)
                    }
                      
                    if (hierLevel === 0) {
                    	fontSize = getFontSize( community.degree, countNorm );
                    }
                    else {
                    	fontSize = getFontSize( community.numNodes, countNorm );
                    }
                    percent = Math.min( 1, (( fontSize - MIN_FONT_SIZE ) / 10) + 0.5 );
                    weight = Math.round( (percent*100) / 10 ) * 10;
                    
                    $html = $html.add( '<div class="node-label node-label-'+this.id+' node-label-'+this.id+'-'+ weight +'" style="'
                          + 'left:'+(offset.x)+'px;'
                          + 'top:'+(offset.y)+'px;'
                          + 'font-size:' + fontSize + 'px;'
                          + 'line-height:' + fontSize + 'px;'
                          + 'margin-top:' + (-fontSize/2) + 'px;'
                          + 'height:' + fontSize + 'px;'
                          + 'z-index:' + Math.floor( fontSize ) + ';'
                          + '">'+split[labelIndex]+'</div>' );

                }
            }

            return $html;
        }


    });

    return GraphNodeRenderer;
});